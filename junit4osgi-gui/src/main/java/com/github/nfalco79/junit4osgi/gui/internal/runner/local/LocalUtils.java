package com.github.nfalco79.junit4osgi.gui.internal.runner.local;

import java.util.Collection;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.github.nfalco79.junit4osgi.gui.SwingRunner;
import com.github.nfalco79.junit4osgi.gui.internal.runner.TestExecutor;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;

public final class LocalUtils {

	public static TestExecutor getExecutor() {
		Bundle bundle = FrameworkUtil.getBundle(SwingRunner.class);
		if (bundle == null) {
			throw new UnsupportedOperationException("You are not in an OSGi enviroment");
		}

		BundleContext bundleContext = bundle.getBundleContext();
		if (bundleContext == null) {
			throw new IllegalStateException("This bundle is not in state RESOLVED or ACTIVE, BundleContext is null");
		}

		try {
			Collection<ServiceReference<TestRegistry>> serviceReferences = bundleContext
					.getServiceReferences(TestRegistry.class, "(discovery=auto)");
			if (!serviceReferences.isEmpty()) {
				TestRegistry registry = bundleContext.getService(serviceReferences.iterator().next());
				return new LocalRunner(registry);
			}
		} catch (InvalidSyntaxException e1) {
		}

		throw new IllegalStateException("No registry service registered in the system");
	}

	private LocalUtils() {
	}
}
