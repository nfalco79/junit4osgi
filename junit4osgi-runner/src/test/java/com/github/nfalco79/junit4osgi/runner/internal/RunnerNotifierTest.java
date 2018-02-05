package com.github.nfalco79.junit4osgi.runner.internal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Set;

import org.example.CustomRunListenerTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.runner.spi.TestRunnerNotifier;

public class RunnerNotifierTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void test_custom_listener() throws Exception {
		LogService logService = mock(LogService.class);

		TestBean test1 = mock(TestBean.class);
		when(test1.getId()).thenReturn("id1");
		when(test1.getTestClass()).thenReturn((Class) CustomRunListenerTest.class);

		Set<TestBean> registryTests = new HashSet<TestBean>();
		registryTests.add(test1);
		String[] testIds = new String[] { test1.getId() };

		TestRegistry registry = mock(TestRegistry.class);
		when(registry.getTests(testIds)).thenReturn(registryTests);

		StartAndStopJUnitRunner runner = new StartAndStopJUnitRunner();
		runner.setLog(logService);
		runner.setRegistry(registry);

		final RunListener runListener = mock(RunListener.class);
		final TestRunnerNotifier runNotifier = mock(TestRunnerNotifier.class);
		when(runNotifier.getRunListener()).thenReturn(runListener);

		runner.start(testIds, folder.newFolder().getAbsolutePath(), runNotifier);
		runner.waitExecution();

		verify(runNotifier).start();
		verify(runNotifier).stop();
		verify(runNotifier).getRunListener();
		verify(runListener).testRunStarted(any(Description.class));
		verify(runListener).testRunFinished(any(Result.class));
		verify(runListener, times(2)).testStarted(any(Description.class));
		verify(runListener, times(2)).testFailure(any(Failure.class));
		verify(runListener, never()).testIgnored(any(Description.class));
	}

}