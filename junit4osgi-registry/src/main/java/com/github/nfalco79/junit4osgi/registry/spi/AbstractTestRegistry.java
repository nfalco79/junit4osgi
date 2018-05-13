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
package com.github.nfalco79.junit4osgi.registry.spi;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.TestRegistryUtils;
import com.github.nfalco79.junit4osgi.registry.internal.JUnit4BundleListener;
import com.github.nfalco79.junit4osgi.registry.internal.asm.ASMUtils;
import com.github.nfalco79.junit4osgi.registry.internal.asm.BundleTestClassVisitor;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent.TestRegistryEventType;

/**
 * This class provides a default common behavior for a {@link TestRegistry}
 * implementations.
 * <p>
 * This abstract class handle in a secure and ThreadSafe way the listeners
 * mechanism and the storage for the {@link TestBean}. The implementation must
 * just provides how to gather tests when a new contributor is
 * registered/unregistered.
 *
 * @author nikolasfalco
 */
public abstract class AbstractTestRegistry implements TestRegistry {

	private LogService log;
	private JUnit4BundleListener bundleListener;

	protected final Set<TestRegistryChangeListener> listeners = new CopyOnWriteArraySet<TestRegistryChangeListener>();
	protected final Map<Bundle, Set<TestBean>> tests = new ConcurrentHashMap<Bundle, Set<TestBean>>();

	public LogService getLog() {
		return log;
	}

	public void setLog(LogService log) {
		this.log = log;
	}

	protected void activate(BundleContext bundleContext) {
		bundleListener = new JUnit4BundleListener(this);
		bundleContext.addBundleListener(bundleListener);
		// parse current bundles
		for (Bundle bundle : bundleContext.getBundles()) {
			bundleListener.addBundle(bundle);
		}
	}

	protected void deactivate(BundleContext bundleContext) {
		try {
			bundleContext.removeBundleListener(bundleListener);
		} finally {
			dispose();
		}
	}

	protected void fireEvent(TestRegistryEvent event) {
		for (TestRegistryChangeListener listener : listeners) {
			try {
				listener.registryChanged(event);
			} catch (Exception t) {
				getLog().log(LogService.LOG_INFO, "Listener " + listener.getClass() //
					+ " fails on event " + event.getType() //
					+ " for the test " + event.getTest().getId());
			}
		}
	}

	protected boolean isTestClass(Bundle bundle, TestBean bean, BundleTestClassVisitor visitor) {
		final String className = bean.getName();

		boolean isTest = false;
		boolean isLazy = "lazy".equals(bundle.getHeaders().get("Bundle-ActivationPolicy"));
		if ((bundle.getState() == Bundle.RESOLVED && !isLazy) || bundle.getState() == Bundle.ACTIVE) {

			final String symbolicName = bundle.getSymbolicName();

			// use classloader to introspect class
			try {
				Class<?> testClass = bean.getTestClass();
				isTest = TestRegistryUtils.isValidTestClass(testClass);
			} catch (ClassNotFoundException e) {
				// could happen if some static code in the class fails
				getLog().log(LogService.LOG_ERROR,
						"Test class '" + className + "' could not be found in the bundle " + symbolicName, e);
			} catch (NoClassDefFoundError e) {
				// happen when miss some import package in MANIFEST.MF
				getLog().log(LogService.LOG_ERROR, "Test class '" + className
						+ "' could not be loaded by its bundle " + symbolicName + " classloader: ", e);
			}
		} else {
			URL entry = bundle.getEntry('/' + className.replace('.', '/') + ".class");
			assert entry != null; // checked by TestBean constructor

			visitor.reset();

			// to avoid triggering of the bundle activation, we will analyse the class byte code
			final String symbolicName = bundle.getSymbolicName();

			// use classloader to introspect class
			try {
				ASMUtils.analyseByteCode(entry, visitor);
				isTest = visitor.isTestClass();
			} catch (RuntimeException e) {
				// could happen if some static code in the class fails
				getLog().log(LogService.LOG_ERROR, "Test class '" + className + "' could not be found in the bundle " + symbolicName, e.getCause());
			} catch (NoClassDefFoundError e) {
				// happen when miss some import package in MANIFEST.MF
				getLog().log(LogService.LOG_ERROR, "Test class '" + className
						+ "' could not be loaded by its bundle " + symbolicName + " classloader: ", e);
			}
		}

		return isTest;
	}

	@Override
	public void dispose() {
		tests.clear();
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.registry.TestRegistry#addTestListener(com.github.nfalco79.junit4osgi.registry.TestRegistryListener)
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
	 * @see com.github.nfalco79.junit4osgi.registry.TestRegistry#removeTestListener(com.github.nfalco79.junit4osgi.registry.TestRegistryListener)
	 */
	@Override
	public void removeTestRegistryListener(TestRegistryChangeListener listener) {
		if (listener == null) {
			throw new NullPointerException("Cannot remove a null listener");
		}
		listeners.remove(listener);
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
	 * @see com.github.nfalco79.junit4osgi.registry.TestRegistry#getTests()
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
	 * @see com.github.nfalco79.junit4osgi.registry.spi.TestRegistry#getTests(java.lang.String[])
	 */
	@Override
	public Set<TestBean> getTests(String[] testIds) {
		Set<TestBean> testBucket = new LinkedHashSet<TestBean>();

		if (testIds != null) {
			for (Set<TestBean> bundleTests : tests.values()) {
				for (TestBean test : bundleTests) {
					for (String testId : testIds) {
						if (testId != null && testId.equals(test.getId())) {
							testBucket.add(test);
							break;
						}
					}
				}
			}
		}
		return testBucket;
	}

}