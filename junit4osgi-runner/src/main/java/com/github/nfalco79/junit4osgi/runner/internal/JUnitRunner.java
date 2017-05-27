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

import java.io.File;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.runner.JUnitCore;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;
import com.github.nfalco79.junit4osgi.runner.spi.TestRunner;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxOperationInfo.OperationAction;
import com.j256.simplejmx.common.JmxResource;

@JmxResource(domainName = "org.osgi.junit4osgi", folderNames = "runner", beanName = "JUnitRunner", description = "The JUnit4 runner, executes JUnit3/4 test case in any OSGi bundle in the current system")
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

	public static final String REPORT_PATH = "org.osgi.junit.reportsPath";
	public static final String RERUN_COUNT = "org.osgi.junit.rerunFailingTestsCount";

	private TestRegistry registry;
	private boolean stop;
	private boolean running;
	private LogService logger;
	private File defaultReportsDirectory;
	private TestRegistryChangeListener testListener;
	private ScheduledThreadPoolExecutor executor;

	public JUnitRunner() {
		defaultReportsDirectory = new File(System.getProperty(REPORT_PATH, "surefire-reports"));
		stop = true;
	}

	/* (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.runner.internal.TestRunner#setRegistry(com.github.nfalco79.junit4osgi.registry.spi.TestRegistry)
	 */
	@Override
	public void setRegistry(final TestRegistry registry) {
		this.registry = registry;
	}

	/* (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.runner.internal.TestRunner#setLog(org.osgi.service.log.LogService)
	 */
	@Override
	public void setLog(LogService logger) {
		this.logger = logger;
	}

	/* (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.runner.internal.TestRunner#start()
	 */
	@Override
	public void start() {
		start(null, null);
	}

	@JmxOperation(description = "Start the runner that execute tests collected by the JUnit registry", operationAction = OperationAction.ACTION)
	public void start(String[] testIds, String reportsPath) {
		if (logger == null || registry == null) {
			return;
		}

		final File reportsDirectory;
		if (reportsPath != null) {
			reportsDirectory = new File(reportsPath);
		} else {
			reportsDirectory = defaultReportsDirectory;
		}

		stop = false;

		if (!isRunning()) {
			final Queue<TestBean> tests;
			if (testIds == null) {
				// create a queue collecting all registry tests
				tests = new ConcurrentLinkedQueue<TestBean>(registry.getTests());
				testListener = new QueeueTestListener(tests);
				registry.addTestRegistryListener(testListener);
			} else {
				// create a queue with only the specified tests
				tests = new ArrayDeque<TestBean>(registry.getTests(testIds));
			}


			running = true;
			executor = new ScheduledThreadPoolExecutor(1);

			Runnable testRunnable = getTestRunnable(reportsDirectory, tests);
			if (testIds != null) {
				executor.schedule(testRunnable, 0l, TimeUnit.MILLISECONDS);
			} else {
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

	protected Runnable getTestRunnable(final File reportsDirectory, final Queue<TestBean> tests) {
		return new Runnable() {
			@Override
			public void run() {
				try {
					running = true;
					runTests(tests, reportsDirectory);
				} finally {
					running = false;
				}
			}
		};
	}

	private void runTests(final Queue<TestBean> tests, final File reportsDirectory) {
		TestBean testBean;
		try {
			while (!isStopped() && (testBean = tests.poll()) != null) {
				try {
					Class<?> testClass = testBean.getTestClass();
					if (!JUnitUtils.hasTests(testClass)) {
						continue;
					}
					JUnitCore core = new JUnitCore();

					// initialise the report listener
					XMLReport report = new XMLReport();
					core.addListener(new ReportListener(report));

					core.run(testClass);

					// write test result
					report.generateReport(reportsDirectory);
				} catch (ClassNotFoundException e) {
					logger.log(LogService.LOG_ERROR, "Cannot load class " + testBean.getId(), e);
				} catch (NoClassDefFoundError e) {
					logger.log(LogService.LOG_ERROR, "Cannot load class " + testBean.getId(), e);
				}
			}
		} catch (Exception e) {
			logger.log(LogService.LOG_ERROR, null, e);
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

}
