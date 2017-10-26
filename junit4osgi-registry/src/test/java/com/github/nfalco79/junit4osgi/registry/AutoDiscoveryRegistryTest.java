package com.github.nfalco79.junit4osgi.registry;

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

import com.github.nfalco79.junit4osgi.registry.internal.AutoDiscoveryRegistry;
import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent.TestRegistryEventType;
import com.github.nfalco79.junit4osgi.registry.util.BundleBuilder;
import com.github.nfalco79.junit4osgi.registry.util.BundleBuilder.URLStrategy;

public class AutoDiscoveryRegistryTest {

	@Test
	public void test_gather_test_by_naming_convention() throws Exception {
		Class<?>[] testsClass = new Class<?>[] { SimpleTestCase.class, JUnit3Test.class, GenericClass.class,
				MyServiceIT.class, SimpleITTest.class, ITGenericClass.class, TestMyService.class,
				MyServiceTests.class };
		Bundle bundle = getMockBundle(testsClass);

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
		Class<?>[] testsClass = new Class<?>[] { TestInnerClassIsNotAJUnit3.class,
				TestInnerClassIsNotAJUnit3.XClass.class, TestOuterIsNotAJUnit3.class,
				TestOuterIsNotAJUnit3.TestInner.class };
		Bundle bundle = getMockBundle(testsClass);

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
		Class<?>[] testsClass = new Class<?>[] { SimpleTestCase.class, JUnit3Test.class };
		Bundle bundle = getMockBundle(testsClass);

		AutoDiscoveryRegistry registry = new AutoDiscoveryRegistry();
		registry.setLog(mock(LogService.class));
		registry.registerTests(bundle);

		assertThat(registry.getTestIds(), Matchers.arrayContainingInAnyOrder("acme@" + SimpleTestCase.class.getName(),
				"acme@" + JUnit3Test.class.getName()));
	}

	@Test
	public void test_that_remove_unregister_only_the_contributor_tests() throws Exception {
		Bundle bundle1 = getMockBundle(new Class<?>[] { SimpleTestCase.class, JUnit3Test.class });
		Bundle bundle2 = getMockBundle(new Class<?>[] { MyServiceIT.class, SimpleITTest.class });

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
		Bundle bundle = getMockBundle(SimpleTestCase.class, JUnit3Test.class);
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

	private Bundle getMockBundle(Class<?>... classes) throws Exception {
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
				}) //
				.build();
	}

}