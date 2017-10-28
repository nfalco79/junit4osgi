/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
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

import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.TestRegistryUtils;
import com.github.nfalco79.junit4osgi.registry.internal.asm.ASMUtils;
import com.github.nfalco79.junit4osgi.registry.internal.asm.BundleTestClassVisitor;
import com.github.nfalco79.junit4osgi.registry.spi.AbstractTestRegistry;
import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent.TestRegistryEventType;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;

@JmxResource(domainName = "org.osgi.junit4osgi", folderNames = "type=registry", beanName = "AutoDiscoveryRegistry", description = "The JUnit4 registry that discovers test using the same maven surefure test naming convention")
public final class AutoDiscoveryRegistry extends AbstractTestRegistry {
	private static final int EXT_LENGHT = ".class".length();

	@JmxOperation(description = "Dispose the registry")
	@Override
	public void dispose() {
		super.dispose();
	}

	@JmxAttributeMethod(description = "Returns a list of all tests id in the registry")
	public String[] getTestIds() {
		Set<String> allTests = new LinkedHashSet<String>();
		for (Set<TestBean> bundleTests : tests.values()) {
			for (TestBean test : bundleTests) {
				allTests.add(test.getId());
			}
		}
		return allTests.toArray(new String[allTests.size()]);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.github.nfalco79.junit4osgi.registry.spi.TestRegistry#registerTests(
	 * org.osgi.framework.Bundle)
	 */
	@Override
	public void registerTests(Bundle bundle) {
		if (tests.containsKey(bundle)) {
			return;
		}

		final String symbolicName = bundle.getSymbolicName();

		Set<TestBean> bundleTest = new LinkedHashSet<TestBean>();
		tests.put(bundle, bundleTest);

		BundleTestClassVisitor visitor = new BundleTestClassVisitor(bundle);

		Enumeration<URL> entries = bundle.findEntries("/", "*.class", true);
		while (entries != null && entries.hasMoreElements()) {
			URL entry = entries.nextElement();
			String className = toClassName(entry);
			String simpleClassName = toClassSimpleName(className);
			if (isTestCase(simpleClassName) || isIntegrationTest(simpleClassName)) {
				TestBean bean = new TestBean(bundle, className);

				boolean isTest = false;
				if (bundle.getState() == Bundle.ACTIVE) {
					// use classloader to introspect class
					try {
						Class<?> testClass = bean.getTestClass();
						isTest = TestRegistryUtils.isValidTestClass(testClass);
					} catch (ClassNotFoundException e) {
						// could happen if some static code in the class fails
						getLog().log(LogService.LOG_ERROR,
								"The class " + className + " could not be found in the bundle " + symbolicName, e);
					} catch (NoClassDefFoundError e) {
						// happen when miss some import package in MANIFEST.MF
						getLog().log(LogService.LOG_ERROR, "The class " + className
								+ " could not be loaded by its bundle " + symbolicName + " classloader: ", e);
					}
				} else {
					// to not trigger the bundle activation, just analyse the
					// class byte code
					visitor.reset();

					ASMUtils.analyseByteCode(entry, visitor);
					isTest = visitor.isTestClass();
				}

				if (isTest) {
					bundleTest.add(bean);

					fireEvent(new TestRegistryEvent(TestRegistryEventType.ADD, bean));
				}
			}
		}
	}

	private String toClassName(URL entry) {
		String className = entry.getPath();
		if (className.startsWith("/")) {
			className = className.substring(1);
		}
		className = className.substring(0, className.length() - EXT_LENGHT);
		return className.replace('/', '.').replace('/', '.');
	}

	private String toClassSimpleName(final String className) {
		int idxInnerClass = className.lastIndexOf('$');
		if (idxInnerClass != -1) {
			return className.substring(idxInnerClass + 1);
		} else {
			return className.substring(className.lastIndexOf('.') + 1);
		}
	}

	private boolean isIntegrationTest(final String className) {
		return className.startsWith("IT") || className.endsWith("IT") || className.endsWith("ITCase");
	}

	private boolean isTestCase(final String className) {
		return className.startsWith("Test") || className.endsWith("Test") || className.endsWith("Tests")
				|| className.endsWith("TestCase");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.github.nfalco79.junit4osgi.registry.spi.TestRegistry#removeTests(org.
	 * osgi.framework.Bundle)
	 */
	@Override
	public void removeTests(Bundle contributor) {
		Set<TestBean> bundleTests = tests.remove(contributor);
		if (bundleTests != null) {
			for (TestBean test : bundleTests) {
				fireEvent(new TestRegistryEvent(TestRegistryEventType.REMOVE, test));
			}
		}
	}

}