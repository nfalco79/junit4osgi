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
package com.github.nfalco79.junit4osgi.registry.internal.spi;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;

import com.github.nfalco79.junit4osgi.registry.internal.util.BundleBuilder;
import com.github.nfalco79.junit4osgi.registry.internal.util.BundleBuilder.URLStrategy;
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

	private Bundle getMockBundle() throws Exception {
		Bundle bundle = BundleBuilder.newBuilder() //
				.symbolicName("acme") //
				.urlStrategy(new URLStrategy() {
					@Override
					public URL resolveURL(Class<?> resource) throws MalformedURLException {
						return null;
					}

					@Override
					public URL resolveURL(String entry) {
						if (entry.startsWith("/")) {
							entry = entry.substring(1);
						}
						return getClass().getClassLoader().getResource(entry);
					}
				})
				.build();
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