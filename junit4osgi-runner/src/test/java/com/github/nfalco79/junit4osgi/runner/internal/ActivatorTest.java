package com.github.nfalco79.junit4osgi.runner.internal;

import static org.powermock.api.mockito.PowerMockito.*;

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.runner.internal.jmx.JMXServer;
import com.github.nfalco79.junit4osgi.runner.spi.TestRunner;

@SuppressWarnings("unchecked")
@RunWith(PowerMockRunner.class)
@PrepareForTest(Activator.class)
public class ActivatorTest {

	@SuppressWarnings("rawtypes")
	private ServiceTrackerCustomizer registryTracker;
	@SuppressWarnings("rawtypes")
	private ServiceTrackerCustomizer logTracker;

	@Test
	public void verify_auto_registry_ldap_filter() throws Exception {
		BundleContext bundleContext = mock(BundleContext.class);
		when(bundleContext.createFilter(ArgumentMatchers.anyString())).thenReturn(mock(Filter.class));

		Activator activator = mockActivator(bundleContext, null, null);

		activator.start(bundleContext);
		Mockito.verify(bundleContext).createFilter(ArgumentMatchers.eq("(&(objectClass=" + TestRegistry.class.getName() + ")(discovery=auto))"));
	}

	@Test
	public void verify_manifest_registry_ldap_filter() throws Exception {
		BundleContext bundleContext = mock(BundleContext.class);
		when(bundleContext.createFilter(ArgumentMatchers.anyString())).thenReturn(mock(Filter.class));

		Activator activator = mockActivator(bundleContext, null, null);

		Properties properties = new Properties();
		properties.putAll(System.getProperties());
		try {
			System.setProperty(Activator.RUNNER_REGISTY, "manifest");
			activator.start(bundleContext);
			Mockito.verify(bundleContext).createFilter(ArgumentMatchers.eq("(&(objectClass=" + TestRegistry.class.getName() + ")(discovery=manifest))"));
		} finally {
			System.setProperties(properties);
		}
	}

	@Test
	public void runner_does_not_start_with_autostart_set_to_false() throws Exception {
		ServiceReference srRegistry = mock(ServiceReference.class);
		ServiceReference srLog = mock(ServiceReference.class);

		BundleContext bundleContext = mock(BundleContext.class);
		when(bundleContext.createFilter(ArgumentMatchers.anyString())).thenReturn(mock(Filter.class));
		when(bundleContext.getService(srRegistry)).thenReturn(mock(TestRegistry.class));
		when(bundleContext.getService(srLog)).thenReturn(mock(LogService.class));

		TestRunner runner = mock(TestRunner.class);
		Activator activator = mockActivator(bundleContext, runner, null);

		activator.start(bundleContext);

		logTracker.addingService(srLog);
		registryTracker.addingService(srRegistry);

		activator.stop(bundleContext);

		Mockito.verify(runner).setLog(ArgumentMatchers.any(LogService.class));
		Mockito.verify(runner).setRegistry(ArgumentMatchers.any(TestRegistry.class));
		Mockito.verify(runner, Mockito.never()).start();
	}

	@Test
	public void runner_starts_if_autostart_set_to_true() throws Exception {
		ServiceReference srRegistry = mock(ServiceReference.class);
		ServiceReference srLog = mock(ServiceReference.class);

		BundleContext bundleContext = mock(BundleContext.class);
		when(bundleContext.createFilter(ArgumentMatchers.anyString())).thenReturn(mock(Filter.class));
		when(bundleContext.getService(srRegistry)).thenReturn(mock(TestRegistry.class));
		when(bundleContext.getService(srLog)).thenReturn(mock(LogService.class));

		TestRunner testRunner = mock(TestRunner.class);
		JMXServer jmxServer = mock(JMXServer.class);
		Activator activator = mockActivator(bundleContext, testRunner, jmxServer);

		Properties properties = new Properties();
		properties.putAll(System.getProperties());
		try {
			System.setProperty(Activator.RUNNER_AUTOSTART, "true");
			activator.start(bundleContext);

			registryTracker.addingService(srRegistry);
			logTracker.addingService(srLog);

			activator.stop(bundleContext);

			Mockito.verify(testRunner).setLog(ArgumentMatchers.any(LogService.class));
			Mockito.verify(testRunner).setRegistry(ArgumentMatchers.any(TestRegistry.class));
			Mockito.verify(testRunner).start();
			Mockito.verify(jmxServer, Mockito.times(2)).register(ArgumentMatchers.any());
			Mockito.verify(jmxServer).start();
		} finally {
			System.setProperties(properties);
		}
	}

	@Test
	public void stop_runner_if_a_required_service_is_gone() throws Exception {
		ServiceReference srRegistry = mock(ServiceReference.class);
		ServiceReference srLog = mock(ServiceReference.class);

		LogService logService = mock(LogService.class);
		TestRegistry registry = mock(TestRegistry.class);

		BundleContext bundleContext = mock(BundleContext.class);
		when(bundleContext.createFilter(ArgumentMatchers.anyString())).thenReturn(mock(Filter.class));
		when(bundleContext.getService(srRegistry)).thenReturn(registry);
		when(bundleContext.getService(srLog)).thenReturn(logService);

		TestRunner runner = mock(TestRunner.class);
		JMXServer jmxServer = mock(JMXServer.class);
		Activator activator = mockActivator(bundleContext, runner, jmxServer);

		Properties properties = new Properties();
		properties.putAll(System.getProperties());
		activator.start(bundleContext);

		logTracker.addingService(srLog);
		registryTracker.addingService(srRegistry);

		Mockito.verify(runner).setLog(ArgumentMatchers.any(LogService.class));
		Mockito.verify(runner).setRegistry(ArgumentMatchers.any(TestRegistry.class));
		Mockito.verify(jmxServer).register(runner);
		Mockito.verify(jmxServer).register(registry);
		Mockito.verify(jmxServer).start();

		logTracker.removedService(srLog, logService);

		Mockito.verify(runner).stop();
		Mockito.verify(jmxServer).unregister(runner);
		Mockito.verify(jmxServer).unregister(registry);
		Mockito.verify(jmxServer).stop();

		activator.stop(bundleContext);
	}

	private Activator mockActivator(BundleContext bundleContext, TestRunner runner, JMXServer jmxServer) throws Exception {
		Activator activator = spy(new Activator());
		doReturn(jmxServer != null ? jmxServer : mock(JMXServer.class)).when(activator, "newJMXServer");
		doReturn(runner != null ? runner : mock(TestRunner.class)).when(activator, "newRunner");
		doAnswer(new Answer<ServiceTrackerCustomizer<TestRegistry, TestRegistry>>() {
			@Override
			public ServiceTrackerCustomizer<TestRegistry, TestRegistry> answer(InvocationOnMock invocation)
					throws Throwable {
				registryTracker = (ServiceTrackerCustomizer<TestRegistry, TestRegistry>) invocation.callRealMethod();
				return registryTracker;
			}
		}).when(activator, "createRegistryCustomizer", bundleContext);
		doAnswer(new Answer<ServiceTrackerCustomizer<LogService, LogService>>() {
			@Override
			public ServiceTrackerCustomizer<LogService, LogService> answer(InvocationOnMock invocation)
					throws Throwable {
				logTracker = (ServiceTrackerCustomizer<LogService, LogService>) invocation.callRealMethod();
				return logTracker;
			}
		}).when(activator, "createLogCustomizer", bundleContext);
		return activator;
	}

}
