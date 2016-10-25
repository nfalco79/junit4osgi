package com.github.nfalco79.junit4osgi.registry.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;

public class JUnit4BundleListener implements BundleListener {
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
		getRegistry().removeTests(bundle);
	}

	private void registerTestCase(Bundle bundle) {
		getRegistry().registerTests(bundle);
	}

	/* package */TestRegistry getRegistry() {
		return registry;
	}

}