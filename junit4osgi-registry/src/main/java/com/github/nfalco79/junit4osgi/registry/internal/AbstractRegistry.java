package com.github.nfalco79.junit4osgi.registry.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;

/*package*/ abstract class AbstractRegistry implements TestRegistry {

	private LogService log;
	private JUnit4BundleListener bundleListener;

	protected LogService getLog() {
		return log;
	}

	protected void setLog(LogService log) {
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

}