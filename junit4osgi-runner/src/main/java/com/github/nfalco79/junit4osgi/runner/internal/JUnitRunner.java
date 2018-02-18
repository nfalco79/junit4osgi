/*
 * Copyright 2017 Nikolas Falco
 * Licensed under the Apache License, Version 2.0 (the
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

import java.io.File;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.TestRegistryUtils;
import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;
import com.github.nfalco79.junit4osgi.runner.internal.AntGlobPattern.IncludeExcludePattern;
import com.github.nfalco79.junit4osgi.runner.internal.jmx.JMXServer;
import com.github.nfalco79.junit4osgi.runner.spi.TestRunner;
import com.github.nfalco79.junit4osgi.runner.spi.TestRunnerNotifier;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxOperationInfo.OperationAction;
import com.j256.simplejmx.common.JmxResource;

@JmxResource(domainName = "org.osgi.junit4osgi", folderNames = "type=runner", beanName = "JUnitRunner", description = "The JUnit4 runner, executes JUnit3/4 test case in any OSGi bundle in the current system")
public class JUnitRunner implements TestRunner {
	private final class QueeueTestListener implements TestRegistryChangeListener {
		private final Queue<TestBean> tests;

		private QueeueTestListener(Queue<TestBean> tests) {
			this.tests = tests;
		}

		@Override
		public void registryChanged(TestRegistryEvent event) {
			TestBean testBean = event.getTest();
			if (testBean == null) {
				throw new IllegalArgumentException("event has a null test bean");
			}
			switch (event.getType()) {
			case ADD:
				tests.add(testBean);
				break;
			case REMOVE:
				tests.remove(testBean);
				break;
			default:
				logger.log(LogService.LOG_WARNING, "Test registry event type " + event.getType() + " not supported");
				break;
			}
		}
	}

	public static final String RUNNER_REGISTY = "org.osgi.junit.runner.registry";
	public static final String RUNNER_AUTOSTART = "org.osgi.junit.runner.autostart";
	public static final String REPORT_PATH = "org.osgi.junit.reportsPath";
	public static final String RERUN_COUNT = "org.osgi.junit.rerunFailingTestsCount";
	public static final String PATH_INCLUDES = "org.osgi.junit.include";
	public static final String PATH_EXCLUDE = "org.osgi.junit.exclude";

	private TestRegistry registry;
	private boolean stop;
	private boolean running;
	private LogService logger;
	private Set<IncludeExcludePattern> includes;
	private Set<IncludeExcludePattern> excludes;
	private TestRegistryChangeListener testListener;
	private ScheduledThreadPoolExecutor executor;
	private Integer reRunCount;
	private final File defaultReportsDirectory;

	public JUnitRunner() {
		defaultReportsDirectory = new File(System.getProperty(REPORT_PATH, "surefire-reports"));
		reRunCount = Integer.getInteger(RERUN_COUNT, 0);
		stop = true;

		setIncludes(getPatterns(PATH_INCLUDES));
		setExcludes(getPatterns(PATH_EXCLUDE));
	}

	private Set<String> getPatterns(final String property) {
		Set<String> patterns = new LinkedHashSet<String>();
		String propertyValue = System.getProperty(property, "");
		if (!"".equals(propertyValue)) {
			String[] pattern = propertyValue.split(",| ");
			if (pattern.length > 0) {
				patterns.addAll(Arrays.asList(pattern));
			}
		}
		return patterns;
	}

	/* (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.runner.internal.TestRunner#setRegistry(com.github.nfalco79.junit4osgi.registry.spi.TestRegistry)
	 */
	@Override
	public void setRegistry(final TestRegistry registry) {
		bindRegistry(registry, null);
	}

	/**
	 * Binds the registry service reference.
	 *
	 * @param registry
	 *            an implementation of {@link TestRegistry}
	 * @param properties
	 *            component declaration properties registered for the given
	 *            registry instance.
	 */
	public void bindRegistry(final TestRegistry registry, final Map<String, Object> properties) {
		final String defaultRegistry = System.getProperty(RUNNER_REGISTY, "auto");

		if (properties == null || defaultRegistry.equals(properties.get("discovery"))) {
			this.registry = registry;
			jmxServer.register(registry);
		}
	}

	/**
	 * Remove binds of the registry service instance if matches the current.
	 *
	 * @param registry
	 *            the implementation of {@link TestRegistry} that is being
	 *            disabled.
	 */
	public void unbindRegistry(final TestRegistry registry) {
		if (this.registry == registry) {
			this.registry = null;
			jmxServer.unregister(registry);
		}
	}

	/* (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.runner.internal.TestRunner#setLog(org.osgi.service.log.LogService)
	 */
	@Override
	public void setLog(final LogService logger) {
		this.logger = logger;
	}

	/* (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.runner.internal.TestRunner#start()
	 */
	@Override
	public void start() {
		start(null, null, null);
	}

	@JmxOperation(description = "Start the runner that execute the test with the specified id collected by the JUnit registry", operationAction = OperationAction.ACTION)
	public void start(String[] testIds, String reportsPath) {
	    start(testIds, reportsPath, null);
	}

	@Override
	public void start(String[] testIds, String reportsPath, TestRunnerNotifier notifier) {
		if (logger == null || registry == null) {
			return;
		}

		final File reportsDirectory;
		if (reportsPath != null) {
			reportsDirectory = new File(reportsPath);
		} else {
			reportsDirectory = defaultReportsDirectory;
		}

		if (!isRunning()) {
			final Queue<TestBean> tests;
			if (testIds == null) {
				// create a queue collecting all registry tests
				tests = new ConcurrentLinkedQueue<TestBean>();
				testListener = new QueeueTestListener(tests);
				registry.addTestRegistryListener(testListener);

				tests.addAll(registry.getTests());
			} else {
				// create a queue with only the specified tests
				tests = new ArrayDeque<TestBean>(registry.getTests(testIds));
			}

			stop = false;
			running = true;
			executor = new ScheduledThreadPoolExecutor(1);

			if (testIds != null) {
				Runnable testRunnable = getSingleRunnable(reportsDirectory, tests, notifier);
				executor.schedule(testRunnable, 0l, TimeUnit.MILLISECONDS);
			} else {
				Runnable testRunnable = getInfiniteRunnable(reportsDirectory, tests);
				executor.scheduleAtFixedRate(testRunnable, 0l, getRepeatTime(), TimeUnit.MILLISECONDS);
			}
		}
	}

	/**
	 * For test purpose only
	 *
	 * @return the delay time before reschedule the job runner
	 */
	protected long getRepeatTime() {
		return 5000l;
	}

	protected Runnable getSingleRunnable(final File reportsDirectory, final Queue<TestBean> tests, final TestRunnerNotifier notifier) {
		return getTestRunnable(reportsDirectory, tests, notifier, true);
	}

	protected Runnable getInfiniteRunnable(final File reportsDirectory, final Queue<TestBean> tests) {
		return getTestRunnable(reportsDirectory, tests, null, false);
	}

	private Runnable getTestRunnable(final File reportsDirectory, final Queue<TestBean> tests, final TestRunnerNotifier notifier, final boolean singleRun) {
		final TestRunnerNotifier safeNotifier = new SafeTestRunnerNotifier(notifier, logger);

		return new Runnable() {
			@Override
			public void run() {
				try {
					safeNotifier.start();
					runTests(tests, reportsDirectory, safeNotifier);
				} finally {
					if (singleRun) {
						running = false;
					}
					safeNotifier.stop();
				}
			}
		};
	}

	private void runTests(final Queue<TestBean> tests, final File reportsDirectory, TestRunnerNotifier notifier) {
		TestBean testBean;
		try {
			RunListener customListener = null;
			ReportListener reportListener = null;
			JUnitCore core = new JUnitCore();

			while (!isStopped() && (testBean = tests.poll()) != null) {
				try {
					Class<?> testClass = testBean.getTestClass();
					if (!TestRegistryUtils.isValidTestClass(testClass) || !accept(testClass)) {
					    logger.log(LogService.LOG_DEBUG, "Skip test class " + testBean.getName());
						continue;
					}

					// initialise the report listener
					reportListener = new ReportListener();
					core.addListener(reportListener);

					customListener = notifier.getRunListener();
					if (customListener != null) {
						core.addListener(customListener);
					}

					logger.log(LogService.LOG_INFO, "Running test " + testBean.getId());
					Request request = Request.classes(testClass);
					Result result = core.run(request);

					if (isRerunFailingTests() && !result.wasSuccessful()) {
						rerunTests(core, reportListener);
					}

					// write test result
					final XMLReport xmlReport = new XMLReport(reportsDirectory);
					xmlReport.generateReport(reportListener.getReport());
				} catch (ClassNotFoundException e) {
					logger.log(LogService.LOG_ERROR, "Cannot load class " + testBean.getId(), e);
				} catch (NoClassDefFoundError e) {
					logger.log(LogService.LOG_ERROR, "Cannot load class " + testBean.getId(), e);
				} finally {
					if (customListener != null) {
						core.removeListener(customListener);
					}
					if (reportListener != null) {
						core.removeListener(reportListener);
					}
				}
			}
		} catch (Exception e) {
			logger.log(LogService.LOG_ERROR, null, e);
		}
	}

	protected void rerunTests(final JUnitCore core, final ReportListener listener) {
		// remove the report listener in case of rerun, will be
		// used a custom listener to avoid reset statistics
		core.removeListener(listener);

		RerunListenerWrapper reportListener = new RerunListenerWrapper(listener);
		try {
			core.addListener(reportListener);

			Collection<Description> failedTests = listener.getFailures();
			for (Description test : failedTests) {
				final Class<?> testClass = test.getTestClass();
				int runCount = reRunCount;
				Result rerunResult = null;
				while (runCount > 0 && (rerunResult == null || !rerunResult.wasSuccessful())) {
					Request request = Request.classes(testClass).filterWith(test);
					runCount--;
					rerunResult = core.run(request);
				}
			}
		} finally {
			core.removeListener(reportListener);
		}
	}

	/* (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.runner.internal.TestRunner#stop()
	 */
	@Override
	@JmxOperation(description = "Stop any active runner", operationAction = OperationAction.ACTION)
	public void stop() {
		stop = true;
		if (registry != null && testListener != null) {
			registry.removeTestRegistryListener(testListener);
		}
		if (executor != null) {
		    running = false;
			executor.shutdownNow();
		}
	}

	/* (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.runner.internal.TestRunner#isStopped()
	 */
	@Override
	@JmxAttributeMethod(description = "Returns if the runner is stopped or is plan to be stopped")
	public boolean isStopped() {
		return stop;
	}

	/* (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.runner.internal.TestRunner#isRunning()
	 */
	@Override
	@JmxAttributeMethod(description = "Returns the actual state of JUnit runner")
	public boolean isRunning() {
		return running;
	}

	public void setIncludes(Set<String> includes) {
		this.includes = new LinkedHashSet<IncludeExcludePattern>(includes.size());
		for (String include : includes) {
			this.includes.add(AntGlobPattern.include(include));
		}
	}

	public void setExcludes(Set<String> excludes) {
		this.excludes = new LinkedHashSet<IncludeExcludePattern>(excludes.size());
		for (String exclude : excludes) {
			this.excludes.add(AntGlobPattern.exclude(exclude));
		}
	}

	public boolean accept(Class<?> testClass) {
		boolean matches = includes.isEmpty(); // by default accepts all
		String suiteName = testClass.getName();

		Iterator<IncludeExcludePattern> include = includes.iterator();
		while (include.hasNext() && !matches) {
			matches = include.next().matches(suiteName);
		}

		Iterator<IncludeExcludePattern> exclude = excludes.iterator();
		while (exclude.hasNext() && matches) {
			if (exclude.next().matches(suiteName)) {
				matches = false;
				logger.log(LogService.LOG_DEBUG, "Test class: " + testClass.getName() + " excluded by exclude pattern");
			}
		}
		return matches;
	}

	public void setRerunFailingTests(Integer count) {
		this.reRunCount = count;
	}

	public boolean isRerunFailingTests() {
		return reRunCount > 0;
	}

	private JMXServer jmxServer = newJMXServer();

	protected JMXServer newJMXServer() {
		return new JMXServer();
	}

	public void activate() {
		jmxServer.start();
		jmxServer.register(this);

		if (Boolean.getBoolean(RUNNER_AUTOSTART)) {
			start();
		}
	}

	protected JMXServer getJMXServer() {
		return jmxServer;
	}

	public void deactivate() {
		stop();

		jmxServer.unregister(this);
		jmxServer.unregister(registry);
		jmxServer.stop();
	}
}