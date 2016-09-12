package com.github.nfalco79.junit4osgi.registry.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.github.nfalco79.junit4osgi.registry.JUnit4BundleListener;
import com.github.nfalco79.junit4osgi.registry.TestSuiteRegistry;

public class Activator implements BundleActivator {

	private TestSuiteRegistry registry;
	private JUnit4BundleListener bundleListener;
	private ServiceRegistration<TestSuiteRegistry> registration;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		registry = new TestSuiteRegistry();

		bundleListener = new JUnit4BundleListener(this.registry);
		// parse current bundles
		for (Bundle bundle : bundleContext.getBundles()) {
			bundleListener.addBundle(bundle);
		}
		bundleContext.addBundleListener(bundleListener);

		registration = bundleContext.registerService(TestSuiteRegistry.class, this.registry, null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		bundleContext.removeBundleListener(this.bundleListener);
		this.registry.dispose();
		this.registry = null;

		registration.unregister();
	}

}