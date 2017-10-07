package com.github.nfalco79.junit4osgi.registry;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.example.GenericClass;
import org.example.JUnit3Test;
import org.example.MyServiceIT;
import org.example.SimpleITTest;
import org.example.SimpleTestCase;
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

public class AutoDiscoveryRegistryTest {

	@Test
	public void test_gather_test_by_naming_convention() throws Exception {
		Class<?>[] testsClass = new Class<?>[] { SimpleTestCase.class, JUnit3Test.class, GenericClass.class,
				MyServiceIT.class, SimpleITTest.class, GenericClass.class };
		Bundle bundle = getMockBundle(testsClass);

		AutoDiscoveryRegistry registry = new AutoDiscoveryRegistry();
		registry.setLog(mock(LogService.class));
		registry.registerTests(bundle);

		Set<TestBean> tests = registry.getTests();
		assertThat(tests, Matchers.hasSize(4));
		assertThat(tests, Matchers.not(Matchers.contains(new TestBean(bundle, GenericClass.class.getName()))));

		registry.dispose();
	}

	@Test
	public void test_jmx_method_get_test_ids() throws Exception {
		Class<?>[] testsClass = new Class<?>[] { SimpleTestCase.class, JUnit3Test.class };
		Bundle bundle = getMockBundle(testsClass);

		AutoDiscoveryRegistry registry = new AutoDiscoveryRegistry();
		registry.setLog(mock(LogService.class));
		registry.registerTests(bundle);

		assertThat(registry.getTestIds(), Matchers.arrayContainingInAnyOrder("acme@" + SimpleTestCase.class.getName(), "acme@" + JUnit3Test.class.getName()));
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
		Bundle bundle = getMockBundle(new Class<?>[] { SimpleTestCase.class, JUnit3Test.class });
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

	private Collection<URL> toURL(Class<?>[] testsClass) throws Exception {
		List<URL> resources = new ArrayList<URL>(testsClass.length);
		for (Class<?> testClass : testsClass) {
			resources.add(new URL("file:///" + toResource(testClass.getName())));
		}
		return resources;
	}

	private String toResource(String clazz) {
		return clazz.replace('.', '/') + ".class";
	}

	private Bundle getMockBundle(Class<?>... classes) throws Exception {
		Bundle bundle = mock(Bundle.class);
		when(bundle.getSymbolicName()).thenReturn("acme");
		when(bundle.findEntries("/", "*.class", true)).thenReturn(new Vector<URL>(toURL(classes)).elements());
		for (Class<?> clazz : classes) {
			String resource = toResource(clazz.getName());
			when(bundle.getEntry(resource)).thenReturn(getClass().getResource('/' + resource));
		}
		return bundle;
	}

}