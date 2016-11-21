package com.github.nfalco79.junit4osgi.registry.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent.TestRegistryEventType;

public final class ManifestRegistry implements TestRegistry {
	private static final String TEST_ENTRY = "Test-Suite";

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
		parseManifest(contributor);
	}

	/* (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.registry.TestRegistry#removeBundleTests(org.osgi.framework.Bundle)
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

	private void parseManifest(Bundle bundle) {
		Set<TestBean> bundleTest = new LinkedHashSet<TestBean>();
		tests.put(bundle, bundleTest);

		final String symbolicName = bundle.getSymbolicName();

		final URL resource = bundle.getEntry("META-INF/MANIFEST.MF");
		if (resource == null) {
			System.out.println("No MANIFEST for bundle " + symbolicName + "[id:" + bundle.getVersion() + "]");
			return;
		}

		InputStream is = null;
		try {
			is = resource.openStream();
			Manifest mf = new Manifest(is);

			// fragments must be handled differently??
			final String value = mf.getMainAttributes().getValue(TEST_ENTRY);
			if (value != null && !"".equals(value)) {
				StringTokenizer st = new StringTokenizer(value, ",");
				while (st.hasMoreTokens()) {
					String testClass = st.nextToken().trim();
					try {
						TestBean bean = new TestBean(bundle, testClass);
						bundleTest.add(bean);

						fireEvent(new TestRegistryEvent(TestRegistryEventType.ADD, bean));
					} catch (IllegalArgumentException e) {
						System.out.println("Test class '" + testClass + "' not found in bundle " + symbolicName);
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Could not read MANIFEST of bundle " + symbolicName);
		} finally {
			closeSilently(is);
		}
	}

	private void closeSilently(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) { // NOSONAR
			}
		}
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