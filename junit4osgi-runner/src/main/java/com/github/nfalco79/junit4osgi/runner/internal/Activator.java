package com.github.nfalco79.junit4osgi.runner.internal;

import org.junit.runner.notification.RunListener;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.github.nfalco79.junit4osgi.registry.TestSuiteRegistry;
import com.github.nfalco79.junit4osgi.runner.JUnitRunner;

public class Activator implements BundleActivator {

	private ServiceTracker<TestSuiteRegistry, TestSuiteRegistry> registryTracker;
	private ServiceTracker<LogService, LogService> logTracker;
	private JUnitRunner runner;
	private TestSuiteRegistry registry;
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

		registryTracker = new ServiceTracker<TestSuiteRegistry, TestSuiteRegistry>(bundleContext, TestSuiteRegistry.class, createRegistryCustomizer(bundleContext));
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

	private ServiceTrackerCustomizer<TestSuiteRegistry, TestSuiteRegistry> createRegistryCustomizer(final BundleContext bundleContext) {
		return new ServiceTrackerCustomizer<TestSuiteRegistry, TestSuiteRegistry>() {
			@Override
			public TestSuiteRegistry addingService(ServiceReference<TestSuiteRegistry> reference) {
				TestSuiteRegistry registryService = bundleContext.getService(reference);
				synchronized (Activator.this) {
					if (registry == null) {
						registry = registryService;
						bind();
					}
				}
				return registry;
			}

			@Override
			public void modifiedService(ServiceReference<TestSuiteRegistry> arg0, TestSuiteRegistry arg1) {
				// No service property modifications to handle
			}

			@Override
			public void removedService(ServiceReference<TestSuiteRegistry> reference, TestSuiteRegistry service) {
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