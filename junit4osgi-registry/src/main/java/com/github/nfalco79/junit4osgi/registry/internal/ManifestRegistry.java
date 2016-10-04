package com.github.nfalco79.junit4osgi.registry.internal;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.Bundle;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent.TestRegistryEventType;

public final class ManifestRegistry implements TestRegistry {
	private final List<TestRegistryChangeListener> listeners = new CopyOnWriteArrayList<TestRegistryChangeListener>();
	private final Map<Bundle, Set<TestBean>> tests = new ConcurrentHashMap<Bundle, Set<TestBean>>();

	public void dispose() {
		tests.clear();
	}

	/* (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.registry.TestRegistry#registerTest(org.osgi.framework.Bundle, java.lang.String)
	 */
	@Override
	public void registerTest(Bundle contributor, String testClass) {
		Set<TestBean> bundleTest = tests.get(contributor);
		if (bundleTest == null) {
			bundleTest = new LinkedHashSet<TestBean>();
			tests.put(contributor, bundleTest);
		}

		TestBean bean = new TestBean(contributor, testClass);
		bundleTest.add(bean);

		fireEvent(new TestRegistryEvent(TestRegistryEventType.ADD, bean));
	}

	/* (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.registry.TestRegistry#removeBundleTests(org.osgi.framework.Bundle)
	 */
	@Override
	public void removeBundleTests(Bundle contributor) {
		Set<TestBean> bundleTests = tests.remove(contributor);
		for (TestBean test : bundleTests) {
			fireEvent(new TestRegistryEvent(TestRegistryEventType.REMOVE, test));
		}
	}

	/* (non-Javadoc)
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

	/* (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.registry.TestRegistry#addTestListener(com.github.nfalco79.junit4osgi.registry.TestRegistryListener)
	 */
	@Override
	public void addTestRegistryListener(TestRegistryChangeListener listener) {
		if (listener == null) {
			throw new NullPointerException("Cannot add a null listener");
		}
		listeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.registry.TestRegistry#removeTestListener(com.github.nfalco79.junit4osgi.registry.TestRegistryListener)
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
				System.out.println("Listener " + listener.getClass() + " fails on event " + event.getType() + " for the test " + event.getTest().getId());
			}
		}
	}

}