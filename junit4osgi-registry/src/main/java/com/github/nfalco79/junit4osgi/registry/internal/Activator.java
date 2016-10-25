package com.github.nfalco79.junit4osgi.registry.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;

public class Activator implements BundleActivator {

	private List<JUnit4BundleListener> bundleListeners = new ArrayList<JUnit4BundleListener>();
	private ServiceRegistration<TestRegistry> registration;

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		// manifest registry
		Dictionary<String, String> properties = new Hashtable<String, String>(1); // NOSONAR
		properties.put("discovery", "manifest");
		registerRegistry(bundleContext, new AutoDiscoveryRegistry(), properties);

		// auto discovery registry
		properties = new Hashtable<String, String>(1); // NOSONAR
		properties.put("discovery", "auto");
		registerRegistry(bundleContext, new AutoDiscoveryRegistry(), properties);
	}

	private void registerRegistry(BundleContext bundleContext, TestRegistry registry, Dictionary<String, String> properties) {
		JUnit4BundleListener bundleListener = new JUnit4BundleListener(registry);
		bundleListeners.add(bundleListener);
		bundleContext.addBundleListener(bundleListener);
		// parse current bundles
		for (Bundle bundle : bundleContext.getBundles()) {
			bundleListener.addBundle(bundle);
		}

		registration = bundleContext.registerService(TestRegistry.class, registry, properties);
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		for (JUnit4BundleListener bundleListener : bundleListeners) {
			bundleContext.removeBundleListener(bundleListener);
			bundleListener.getRegistry().dispose();
		}

		registration.unregister();
	}

}