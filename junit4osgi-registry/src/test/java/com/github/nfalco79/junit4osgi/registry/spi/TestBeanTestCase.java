package com.github.nfalco79.junit4osgi.registry.spi;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.net.URL;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;

public class TestBeanTestCase {

	@Test
	public void test_id() throws Exception {
		String testClass = getClass().getName();
		Bundle bundle = getMockBundle();

		TestBean testBean = new TestBean(bundle, testClass);
		assertEquals("wrong id", "acme@" + testClass, testBean.getId());
	}

	@Test
	public void test_name() throws Exception {
		String testClass = getClass().getName();
		Bundle bundle = getMockBundle();

		TestBean testBean = new TestBean(bundle, testClass);
		assertEquals("wrong name", testClass, testBean.getName());
	}

	@Test
	public void test_class_name() throws Exception {
		String testClass = getClass().getName();
		Bundle bundle = getMockBundle();

		TestBean testBean = new TestBean(bundle, testClass);
		assertEquals("wrong class", getClass(), testBean.getTestClass());
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_invalid_test_for_bundle() throws Exception {
		String testClass = "org.acme.Foo";
		Bundle bundle = getMockBundle();

		new TestBean(bundle, testClass);
	}

	@Test(expected = NullPointerException.class)
	public void test_invalid_bundle() throws Exception {
		new TestBean(null, "org.acme.Foo");
	}

	@Test(expected = NullPointerException.class)
	public void test_invalid_test_class() throws Exception {
		new TestBean(getMockBundle(), null);
	}

	@Test
	public void test_equals_and_hashcode() throws Exception {
		String testClass = getClass().getName();
		Bundle bundle = getMockBundle();

		TestBean bean1 = new TestBean(bundle, testClass);
		TestBean bean2 = new TestBean(bundle, TestBean.class.getName());
		TestBean bean3 = new TestBean(bundle, testClass);

		assertNotEquals(bean1, bean2);
		assertEquals(bean1, bean3);

		assertNotEquals(bean1.hashCode(), bean2.hashCode());
		assertEquals(bean1.hashCode(), bean3.hashCode());
	}

	private Bundle getMockBundle() throws ClassNotFoundException {
		Bundle bundle = mock(Bundle.class);
		when(bundle.getSymbolicName()).thenReturn("acme");
		when(bundle.getEntry(anyString())).thenAnswer(new Answer<URL>() {
			@Override
			public URL answer(InvocationOnMock invocation) throws Throwable {
				String entry = invocation.getArgument(0);
				return getClass().getClassLoader().getResource(entry);
			}
		});
		when(bundle.loadClass(anyString())).thenAnswer(new Answer<Class<?>>() {
			@Override
			public Class<?> answer(InvocationOnMock invocation) throws Throwable {
				String testClass = invocation.getArgument(0);
				return getClass().getClassLoader().loadClass(testClass);
			}
		});
		return bundle;
	}

}