/*
 * Copyright 2017 Nikolas Falco
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.nfalco79.junit4osgi.registry.internal;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.example.JUnit3Test;
import org.example.MyServiceIT;
import org.example.SimpleITTest;
import org.example.SimpleTestCase;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.io.IOUtil;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.internal.util.BundleBuilder;
import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent.TestRegistryEventType;

public class ManifestRegistryTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void jar_with_missing_manifest() throws Exception {
		LogService logService = spy(LogService.class);

		ManifestRegistry registry = new ManifestRegistry();
		registry.setLog(logService);
		registry.registerTests(getMockBundle());

		assertThat(registry.getTests(), Matchers.empty());
		verify(logService).log(eq(LogService.LOG_WARNING), contains("No MANIFEST for bundle"));

		registry.dispose();
	}

	@Test
	public void testclass_not_found() throws Exception {
		LogService logService = spy(LogService.class);

		String className = "com.acme.Foo";
		Bundle bundle = BundleBuilder.newBuilder() //
				.symbolicName("acme") //
				.addResource("META-INF/MANIFEST.MF", getManifest(className)) //
				.build();

		ManifestRegistry registry = new ManifestRegistry();
		registry.setLog(logService);
		registry.registerTests(bundle);

		assertThat(registry.getTests(), Matchers.empty());
		String expectedLog = "Test class '" + className + "' not found in bundle " + bundle.getSymbolicName();
		verify(logService).log(eq(LogService.LOG_ERROR), contains(expectedLog), any(Exception.class));

		registry.dispose();
	}

	@Test
	public void registry_must_returns_the_expected_tests() throws Exception {
		TestRegistryChangeListener listener = spy(TestRegistryChangeListener.class);
		Class<?> testsClass[] = new Class<?>[] { SimpleTestCase.class, JUnit3Test.class };
		Bundle bundle = getMockBundle(testsClass);

		ManifestRegistry registry = new ManifestRegistry();
		registry.setLog(mock(LogService.class));
		registry.addTestRegistryListener(listener);
		registry.registerTests(bundle);

		Set<TestBean> tests = registry.getTests();
		assertThat(tests, Matchers.hasSize(2));
		assertThat(tests, Matchers.hasItems(new TestBean(bundle, testsClass[0].getName()), new TestBean(bundle, testsClass[1].getName())));

		ArgumentCaptor<TestRegistryEvent> argument = ArgumentCaptor.forClass(TestRegistryEvent.class);
		verify(listener, times(2)).registryChanged(argument.capture());
		for (TestRegistryEvent event : argument.getAllValues()) {
			assertThat(event.getType(), Matchers.is(TestRegistryEventType.ADD));
		}

		registry.dispose();
	}

	@Test
	public void remove_a_contributor_fires_a_remove_event() throws Exception {
		Bundle bundle = getMockBundle(SimpleTestCase.class, JUnit3Test.class);

		ManifestRegistry registry = new ManifestRegistry();
		registry.setLog(mock(LogService.class));
		registry.registerTests(bundle);

		TestRegistryChangeListener listener = spy(TestRegistryChangeListener.class);
		registry.addTestRegistryListener(listener);
		registry.removeTests(bundle);

		ArgumentCaptor<TestRegistryEvent> argument = ArgumentCaptor.forClass(TestRegistryEvent.class);
		verify(listener, times(2)).registryChanged(argument.capture());
		for (TestRegistryEvent event : argument.getAllValues()) {
			assertThat(event.getType(), Matchers.is(TestRegistryEventType.REMOVE));
		}

		registry.dispose();
	}

	@Test
	public void remove_a_contributor_removes_only_tests_contributed_by_that_bundle() throws Exception {
		Bundle bundle1 = getMockBundle(SimpleTestCase.class, JUnit3Test.class);
		Bundle bundle2 = getMockBundle(MyServiceIT.class, SimpleITTest.class);

		ManifestRegistry registry = new ManifestRegistry();
		registry.setLog(mock(LogService.class));
		registry.registerTests(bundle1);
		registry.registerTests(bundle2);

		assertThat(registry.getTests(), Matchers.hasSize(4));

		registry.removeTests(bundle1);
		assertThat(registry.getTests(), Matchers.hasSize(2));
		assertThat(registry.getTests(), Matchers.hasItems(new TestBean(bundle2, MyServiceIT.class.getName()), new TestBean(bundle2, SimpleITTest.class.getName())));

		registry.dispose();
	}

	private File getManifest(String... className) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (String clazz : className) {
			sb.append(clazz).append(",");
		}
		Manifest mf = new Manifest();
		Attributes mainAttributes = mf.getMainAttributes();
		mainAttributes.putValue("Manifest-Version", "1.0");
		mainAttributes.putValue(ManifestRegistry.TEST_ENTRY, sb.toString());

		File manifestFile = folder.newFile();
		OutputStream fos = new FileOutputStream(manifestFile);
		try {
			mf.write(fos);
		} finally {
			IOUtil.closeQuietly(fos);
		}
		return manifestFile;
	}

	private Bundle getMockBundle(final Class<?>... testsClass) throws Exception {
		BundleBuilder builder = BundleBuilder.newBuilder() //
				.symbolicName("acme") //
				.addClasses(testsClass);

		if (testsClass != null && testsClass.length > 0) {
			String[] classesName = new String[testsClass.length];
			for (int i = 0; i < testsClass.length; i++) {
				classesName[i] = testsClass[i].getName();
			}

			builder.addResource("META-INF/MANIFEST.MF", getManifest(classesName));
		}
		return builder.build();
	}

}