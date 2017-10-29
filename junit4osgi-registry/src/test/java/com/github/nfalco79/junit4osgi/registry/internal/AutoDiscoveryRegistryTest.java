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
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import org.example.GenericClass;
import org.example.ITGenericClass;
import org.example.JUnit3Test;
import org.example.MyServiceIT;
import org.example.MyServiceTests;
import org.example.SimpleITTest;
import org.example.SimpleTestCase;
import org.example.TestMyService;
import org.example.inner.TestInnerClassIsNotAJUnit3;
import org.example.inner.TestInnerClassIsNotAJUnit3.XClass;
import org.example.inner.TestOuterIsNotAJUnit3;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.internal.util.BundleBuilder;
import com.github.nfalco79.junit4osgi.registry.internal.util.BundleBuilder.URLStrategy;
import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent.TestRegistryEventType;

public class AutoDiscoveryRegistryTest {

	@Test
	public void test_gather_test_by_naming_convention() throws Exception {
		Bundle bundle = getMockBundle(SimpleTestCase.class, JUnit3Test.class, GenericClass.class, MyServiceIT.class,
				SimpleITTest.class, ITGenericClass.class, TestMyService.class, MyServiceTests.class).build();

		AutoDiscoveryRegistry registry = new AutoDiscoveryRegistry();

		registry.setLog(mock(LogService.class));
		registry.registerTests(bundle);

		Set<TestBean> tests = registry.getTests();
		assertThat(tests, Matchers.hasSize(6));
		assertThat(tests, Matchers.not(Matchers.contains(new TestBean(bundle, GenericClass.class.getName()))));

		registry.dispose();
	}

	@Test
	public void test_naming_convention_on_inner() throws Exception {
		Bundle bundle = getMockBundle(TestInnerClassIsNotAJUnit3.class, TestInnerClassIsNotAJUnit3.XClass.class,
				TestOuterIsNotAJUnit3.class, TestOuterIsNotAJUnit3.TestInner.class).build();

		AutoDiscoveryRegistry registry = new AutoDiscoveryRegistry();

		registry.setLog(mock(LogService.class));
		registry.registerTests(bundle);

		Set<TestBean> tests = registry.getTests();
		assertThat(tests, Matchers.hasSize(2));
		assertThat(tests, Matchers.not(Matchers.contains(new TestBean(bundle, XClass.class.getName()),
				new TestBean(bundle, TestOuterIsNotAJUnit3.class.getName()))));

		registry.dispose();
	}

	@Test
	public void test_naming_convention_on_inner_when_state_is_ACTIVE() throws Exception {
		Bundle bundle = getMockBundle(TestInnerClassIsNotAJUnit3.class, TestInnerClassIsNotAJUnit3.XClass.class,
				TestOuterIsNotAJUnit3.class, TestOuterIsNotAJUnit3.TestInner.class) //
						.state(Bundle.ACTIVE) //
						.build();

		AutoDiscoveryRegistry registry = new AutoDiscoveryRegistry();

		registry.setLog(mock(LogService.class));
		registry.registerTests(bundle);

		Set<TestBean> tests = registry.getTests();
		assertThat(tests, Matchers.hasSize(2));
		assertThat(tests, Matchers.not(Matchers.contains(new TestBean(bundle, XClass.class.getName()),
				new TestBean(bundle, TestOuterIsNotAJUnit3.class.getName()))));

		registry.dispose();
	}

	@Test
	public void test_jmx_method_get_test_ids() throws Exception {
		Bundle bundle = getMockBundle(SimpleTestCase.class, JUnit3Test.class).build();

		AutoDiscoveryRegistry registry = new AutoDiscoveryRegistry();
		registry.setLog(mock(LogService.class));
		registry.registerTests(bundle);

		assertThat(registry.getTestIds(), Matchers.arrayContainingInAnyOrder("acme@" + SimpleTestCase.class.getName(),
				"acme@" + JUnit3Test.class.getName()));
	}

	@Test
	public void test_that_remove_unregister_only_the_contributor_tests() throws Exception {
		Bundle bundle1 = getMockBundle(SimpleTestCase.class, JUnit3Test.class).build();
		Bundle bundle2 = getMockBundle(MyServiceIT.class, SimpleITTest.class).build();

		AutoDiscoveryRegistry registry = new AutoDiscoveryRegistry();
		registry.setLog(mock(LogService.class));
		registry.registerTests(bundle1);
		registry.registerTests(bundle2);

		assertThat(registry.getTests(), Matchers.hasSize(4));

		registry.registerTests(bundle1);
		assertThat(registry.getTests(), Matchers.hasSize(4));

		registry.removeTests(bundle1);
		assertThat(registry.getTests(), Matchers.hasSize(2));
		assertThat(registry.getTests(), Matchers.hasItems(new TestBean(bundle2, MyServiceIT.class.getName()),
				new TestBean(bundle2, SimpleITTest.class.getName())));

		registry.dispose();
	}

	@Test
	public void test_listener_event() throws Exception {
		Bundle bundle = getMockBundle(SimpleTestCase.class, JUnit3Test.class).build();
		TestRegistryChangeListener listener = spy(TestRegistryChangeListener.class);

		AutoDiscoveryRegistry registry = new AutoDiscoveryRegistry();

		registry.setLog(mock(LogService.class));
		registry.addTestRegistryListener(listener);

		registry.registerTests(bundle);

		ArgumentCaptor<TestRegistryEvent> argument = ArgumentCaptor.forClass(TestRegistryEvent.class);
		verify(listener, times(2)).registryChanged(argument.capture());
		for (TestRegistryEvent event : argument.getAllValues()) {
			assertThat(event.getType(), Matchers.is(TestRegistryEventType.ADD));
		}

		reset(listener);

		registry.removeTests(bundle);

		argument = ArgumentCaptor.forClass(TestRegistryEvent.class);
		verify(listener, times(2)).registryChanged(argument.capture());
		for (TestRegistryEvent event : argument.getAllValues()) {
			assertThat(event.getType(), Matchers.is(TestRegistryEventType.REMOVE));
		}

		registry.dispose();
	}

	private BundleBuilder getMockBundle(Class<?>... classes) throws Exception {
		return BundleBuilder.newBuilder() //
				.symbolicName("acme") //
				.addClasses(classes) //
				.urlStrategy(new URLStrategy() {
					@Override
					public URL resolveURL(Class<?> resource) throws MalformedURLException {
						String resourcePath = resource.getName().replace('.', '/') + ".class";
						return new URL("testentry", (String) null, 0,
								resourcePath + "?url=" + resource.getResource("/" + resourcePath),
								new TestURLStreamHandler());
					}

					@Override
					public URL resolveURL(String entry) {
						return null;
					}
				});
	}

}