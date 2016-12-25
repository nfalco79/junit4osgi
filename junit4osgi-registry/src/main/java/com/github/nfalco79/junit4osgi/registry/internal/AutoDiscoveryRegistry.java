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
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent.TestRegistryEventType;

public final class AutoDiscoveryRegistry extends AbstractRegistry {
	private static final int EXT_LENGHT = ".class".length();

	private final List<TestRegistryChangeListener> listeners = new CopyOnWriteArrayList<TestRegistryChangeListener>();
	private final Map<Bundle, Set<TestBean>> tests = new ConcurrentHashMap<Bundle, Set<TestBean>>();

	@Override
	public void dispose() {
		tests.clear();
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.registry.spi.TestRegistry#registerTests(org.osgi.framework.Bundle)
	 */
	@Override
	public void registerTests(Bundle contributor) {
		Set<TestBean> bundleTest = new LinkedHashSet<TestBean>();
		tests.put(contributor, bundleTest);

		Enumeration<URL> entries = contributor.findEntries("/", "*.class", true);
		while (entries != null && entries.hasMoreElements()) {
			String className = toClassName(entries.nextElement());
			if (isTestCase(className) || isIntegrationTest(className)) {
				TestBean bean = new TestBean(contributor, className);
				bundleTest.add(bean);

				fireEvent(new TestRegistryEvent(TestRegistryEventType.ADD, bean));
			}
		}
	}

	private String toClassName(URL entry) {
		String className = entry.getFile();
		if (className.startsWith("/")) {
			className = className.substring(1);
		}
		className = className.substring(0, className.length() - EXT_LENGHT);
		return className.replace('/', '.').replace('/', '.');
	}

	private boolean isIntegrationTest(String className) {
		return className.startsWith("IT") || className.endsWith("IT") || className.endsWith("ITCase");
	}

	private boolean isTestCase(String className) {
		return className.startsWith("Test") || className.endsWith("Test") || className.endsWith("TestCase");
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.registry.spi.TestRegistry#removeTests(org.osgi.framework.Bundle)
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

	/*
	 * (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.registry.spi.TestRegistry#getTests()
	 */
	@Override
	public Set<TestBean> getTests() {
		Set<TestBean> allTests = new LinkedHashSet<TestBean>();
		for (Set<TestBean> foo : tests.values()) {
			allTests.addAll(foo);
		}
		return Collections.unmodifiableSet(allTests);
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.registry.spi.TestRegistry#addTestRegistryListener(com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener)
	 */
	@Override
	public void addTestRegistryListener(TestRegistryChangeListener listener) {
		if (listener == null) {
			throw new NullPointerException("Cannot add a null listener");
		}
		listeners.add(listener);
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.registry.spi.TestRegistry#removeTestRegistryListener(com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener)
	 */
	@Override
	public void removeTestRegistryListener(TestRegistryChangeListener listener) {
		if (listener == null) {
			throw new NullPointerException("Cannot remove a null listener");
		}
		listeners.remove(listener);
	}

	private void fireEvent(TestRegistryEvent event) {
		for (TestRegistryChangeListener listener : listeners) {
			try {
				listener.registryChanged(event);
			} catch (Exception t) {
				getLog().log(LogService.LOG_ERROR, "Listener " + listener.getClass()
						+ " fails on event " + event.getType()
						+ " for the test " + event.getTest().getId());
			}
		}
	}

}