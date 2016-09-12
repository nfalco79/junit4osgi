package com.github.nfalco79.junit4osgi.registry;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.Bundle;

import com.github.nfalco79.junit4osgi.registry.TestEvent.TestEventType;

public final class TestSuiteRegistry {
	private final List<TestRegistryListener> listeners = new CopyOnWriteArrayList<TestRegistryListener>();
	private final Map<Bundle, Set<TestBean>> tests = new ConcurrentHashMap<Bundle, Set<TestBean>>();

	public void dispose() {
		tests.clear();
	}

	public void registerTest(Bundle contributor, String testClass) {
		Set<TestBean> bundleTest = tests.get(contributor);
		if (bundleTest == null) {
			bundleTest = new LinkedHashSet<TestBean>();
			tests.put(contributor, bundleTest);
		}

		TestBean bean = new TestBean(contributor, testClass);
		bundleTest.add(bean);

		fireEvent(new TestEvent(TestEventType.ADD, bean));
	}

	public void removeBundleTests(Bundle contributor) {
		Set<TestBean> bundleTests = tests.remove(contributor);
		for (TestBean test : bundleTests) {
			fireEvent(new TestEvent(TestEventType.REMOVE, test));
		}
	}

	public Set<TestBean> getTests() {
		Set<TestBean> allTests = new LinkedHashSet<TestBean>();
		for (Set<TestBean> foo : tests.values()) {
			allTests.addAll(foo);
		}
		return Collections.unmodifiableSet(allTests);
	}

	public void addTestListener(TestRegistryListener listener) {
		if (listener == null) {
			throw new NullPointerException("Cannot add a null listener");
		}
		listeners.add(listener);
	}

	public void removeTestListener(TestRegistryListener listener) {
		if (listener == null) {
			throw new NullPointerException("Cannot remove a null listener");
		}
		listeners.remove(listener);
	}

	private void fireEvent(TestEvent event) {
		for (TestRegistryListener listener : listeners) {
			try {
				switch (event.getType()) {
				case ADD:
					listener.added(event.getTest());
					break;
				case REMOVE:
					listener.removed(event.getTest());
					break;
				default:
					break;
				}
			} catch (Exception t) {
				System.out.println("Listener " + listener.getClass() + " fails on event " + event.getType() + " for the test " + event.getTest().getId());
			}
		}
	}

}