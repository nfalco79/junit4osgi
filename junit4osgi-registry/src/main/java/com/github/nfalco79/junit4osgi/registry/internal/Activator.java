package com.github.nfalco79.junit4osgi.registry.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;

public class Activator implements BundleActivator {

	private ManifestRegistry registry;
	private JUnit4BundleListener bundleListener;
	private ServiceRegistration<TestRegistry> registration;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		registry = new ManifestRegistry();

		bundleListener = new JUnit4BundleListener(this.registry);
		// parse current bundles
		for (Bundle bundle : bundleContext.getBundles()) {
			bundleListener.addBundle(bundle);
		}
		bundleContext.addBundleListener(bundleListener);

		Dictionary<String, String> properties = new Hashtable<String, String>(1);
		properties.put("discovery", "manifest");
		registration = bundleContext.registerService(TestRegistry.class, this.registry, properties);
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