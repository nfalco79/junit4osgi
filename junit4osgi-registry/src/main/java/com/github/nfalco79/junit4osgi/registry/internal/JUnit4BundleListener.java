package com.github.nfalco79.junit4osgi.registry.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;

public class JUnit4BundleListener implements BundleListener {
	private static final String TEST_ENTRY = "Test-Suite";

	private TestRegistry registry;

	public JUnit4BundleListener(TestRegistry registry) {
		this.registry = registry;
	}

	public void addBundle(Bundle bundle) {
		switch (bundle.getState())  {
		case Bundle.ACTIVE:
		case Bundle.INSTALLED:
			registerTestCase(bundle);
			break;
		case Bundle.STOPPING:
		case Bundle.UNINSTALLED:
			unregisterTestCase(bundle);
			break;
		default:
			break;
		}
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		Bundle bundle = event.getBundle();
		switch (event.getType())  {
			case Bundle.ACTIVE:
				registerTestCase(bundle);
				break;
			case Bundle.STOPPING:
				unregisterTestCase(bundle);
				break;
			default:
				break;
		}
	}

	private void unregisterTestCase(Bundle bundle) {
		registry.removeTests(bundle);
	}

	private void registerTestCase(Bundle bundle) {
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
						registry.registerTest(bundle, testClass);
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

}
