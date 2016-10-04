package com.github.nfalco79.junit4osgi.runner.internal;

import org.junit.runner.notification.RunListener;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.github.nfalco79.junit4osgi.registry.internal.ManifestRegistry;
import com.github.nfalco79.junit4osgi.runner.JUnitRunner;

public class Activator implements BundleActivator {

	private ServiceTracker<ManifestRegistry, ManifestRegistry> registryTracker;
	private ServiceTracker<LogService, LogService> logTracker;
	private JUnitRunner runner;
	private ManifestRegistry registry;
	private RunListener listener;
	private LogService logger;
	private ServiceTracker<RunListener, RunListener> listenerTracker;

	private void bind() {
		if (registry == null) {
			registry = registryTracker.getService();
			if (registry == null) {
				return; // missing required service
			}
		}
		if (logger == null) {
			logger = logTracker.getService();
			if (logger == null) {
				return; // missing required service
			}
		}

		runner.setLogger(logger);
		runner.setRegistry(registry);
		runner.setListener(listener);
		runner.startup();
	}

	private void unbind() {
		if (logger  == null || registry == null) {
			return;
		}

		runner.shutdown();
		logger = null;
		registry = null;
		listener = null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		runner = new JUnitRunner();

		registryTracker = new ServiceTracker<ManifestRegistry, ManifestRegistry>(bundleContext, ManifestRegistry.class, createRegistryCustomizer(bundleContext));
		logTracker = new ServiceTracker<LogService, LogService>(bundleContext, LogService.class, createLogCustomizer(bundleContext));
		listenerTracker = new ServiceTracker<RunListener, RunListener>(bundleContext, RunListener.class, createListenerCustomizer(bundleContext));

		registryTracker.open();
		logTracker.open();
		listenerTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		registryTracker.close();
		logTracker.close();
		listenerTracker.close();
	}

	private ServiceTrackerCustomizer<RunListener, RunListener> createListenerCustomizer(final BundleContext bundleContext) {
		return new ServiceTrackerCustomizer<RunListener, RunListener>() {
			@Override
			public RunListener addingService(ServiceReference<RunListener> reference) {
				RunListener listenerService = bundleContext.getService(reference);
				synchronized (Activator.this) {
					if (listener == null) {
						listener = listenerService;
						bind();
					}
				}
				return listener;
			}

			@Override
			public void modifiedService(ServiceReference<RunListener> reference, RunListener service) {
				// No service property modifications to handle
			}

			@Override
			public void removedService(ServiceReference<RunListener> reference, RunListener service) {
				synchronized (Activator.this) {
					if (service != listener) {
						return;
					}
					unbind();
					bind();
				}
			}
		};
	}

	private ServiceTrackerCustomizer<LogService, LogService> createLogCustomizer(final BundleContext bundleContext) {
		return new ServiceTrackerCustomizer<LogService, LogService>() {
			@Override
			public LogService addingService(ServiceReference<LogService> reference) {
				LogService logService = bundleContext.getService(reference);
				synchronized (Activator.this) {
					if (logger == null) {
						logger = logService;
						bind();
					}
				}
				return logService;
			}

			@Override
			public void modifiedService(ServiceReference<LogService> reference, LogService service) {
				// No service property modifications to handle
			}

			@Override
			public void removedService(ServiceReference<LogService> reference, LogService service) {
				synchronized (Activator.this) {
					if (service != logger) {
						return;
					}
					unbind();
					bind();
				}
			}
		};
	}

	private ServiceTrackerCustomizer<ManifestRegistry, ManifestRegistry> createRegistryCustomizer(final BundleContext bundleContext) {
		return new ServiceTrackerCustomizer<ManifestRegistry, ManifestRegistry>() {
			@Override
			public ManifestRegistry addingService(ServiceReference<ManifestRegistry> reference) {
				ManifestRegistry registryService = bundleContext.getService(reference);
				synchronized (Activator.this) {
					if (registry == null) {
						registry = registryService;
						bind();
					}
				}
				return registry;
			}

			@Override
			public void modifiedService(ServiceReference<ManifestRegistry> arg0, ManifestRegistry arg1) {
				// No service property modifications to handle
			}

			@Override
			public void removedService(ServiceReference<ManifestRegistry> reference, ManifestRegistry service) {
				synchronized (Activator.this) {
					if (service != registry) {
						return;
					}
					unbind();
					bind();
				}
			}
		};
	}

}