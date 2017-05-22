/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.nfalco79.junit4osgi.runner.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.runner.internal.jmx.JMXServer;
import com.github.nfalco79.junit4osgi.runner.spi.TestRunner;

public class Activator implements BundleActivator {

	public static final String RUNNER_REGISTY = "org.osgi.junit.runner.registry";
	public static final String RUNNER_AUTOSTART = "org.osgi.junit.runner.autostart";

	private ServiceTracker<TestRegistry, TestRegistry> registryTracker;
	private ServiceTracker<LogService, LogService> logTracker;
	private TestRunner runner;
	private TestRegistry registry;
	private LogService logger;
	private boolean runnerStart;
	private JMXServer jmxServer;
//	private ServiceTracker<RunListener, RunListener> listenerTracker;

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

		runner.setLog(logger);
		runner.setRegistry(registry);

		jmxServer.start();
		jmxServer.register(registry);
		jmxServer.register(runner);

		if (runnerStart) {
			runner.start();
		}
	}

	private void unbind() {
		if (logger == null || registry == null) {
			return;
		}

		runner.stop();

		jmxServer.unregister(runner);
		jmxServer.unregister(registry);
		jmxServer.stop();

		logger = null;
		registry = null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		final String runnerRegistry = System.getProperty(RUNNER_REGISTY, "auto");
		runnerStart = Boolean.getBoolean(RUNNER_AUTOSTART);

		jmxServer = new JMXServer();

		runner = new JUnitRunner();

		registryTracker = new ServiceTracker<TestRegistry, TestRegistry>(bundleContext,
				bundleContext.createFilter("(discovery=" + runnerRegistry + ")"),
				createRegistryCustomizer(bundleContext));
		logTracker = new ServiceTracker<LogService, LogService>(bundleContext,
				LogService.class,
				createLogCustomizer(bundleContext));

		registryTracker.open();
		logTracker.open();
//		listenerTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		registryTracker.close();
		logTracker.close();
//		listenerTracker.close();

		jmxServer.stop();
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

	private ServiceTrackerCustomizer<TestRegistry, TestRegistry> createRegistryCustomizer(
			final BundleContext bundleContext) {
		return new ServiceTrackerCustomizer<TestRegistry, TestRegistry>() {
			@Override
			public TestRegistry addingService(ServiceReference<TestRegistry> reference) {
				TestRegistry registryService = bundleContext.getService(reference);
				synchronized (Activator.this) {
					if (registry == null) {
						registry = registryService;
						bind();
					}
				}
				return registry;
			}

			@Override
			public void modifiedService(ServiceReference<TestRegistry> arg0, TestRegistry arg1) {
				// No service property modifications to handle
			}

			@Override
			public void removedService(ServiceReference<TestRegistry> reference, TestRegistry service) {
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