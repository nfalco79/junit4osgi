package com.github.nfalco79.junit4osgi.runner.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;

public class JUnitRunner {
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

	private TestRegistry registry;
	private boolean stop;
	private boolean running;
	private LogService logger;
	private File reportsDirectory;
	private TestRegistryChangeListener testListener;

	private ScheduledThreadPoolExecutor executor;

	public JUnitRunner() {
		reportsDirectory = new File(System.getProperty(REPORT_PATH, "surefire-reports"));
	}

	public void setRegistry(TestRegistry registry) {
		this.registry = registry;
	}

	public void setLogger(LogService logger) {
		this.logger = logger;
	}

	public void startup() {
		if (logger == null || registry == null) {
			throw new IllegalStateException("One of logger, listener or test registry service component is missing");
		}

		stop = false;

		final Queue<TestBean> tests = new ConcurrentLinkedQueue<TestBean>(registry.getTests());
		testListener = new QueeueTestListener(tests);
		registry.addTestRegistryListener(testListener);

		if (!isRunning()) {
			running = true;
			executor = new ScheduledThreadPoolExecutor(1);
			executor.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					running = true;
					runTests(tests);
					running = false;
				}
			}, 0l, 5, TimeUnit.SECONDS);
		}

	}

	private void runTests(final Queue<TestBean> tests) {
		TestBean testBean;
		try {
			while (!isStopped() && (testBean = tests.poll()) != null) {
				try {
					Class<?> testClass = testBean.getTestClass();
					JUnitCore core = new JUnitCore();

					// inititialise the report listener
					XMLReport report = new XMLReport();
					core.addListener(new ReportListener(report));

					Result result = core.run(testClass);
					// write test result
					report.generateReport(testBean, result, reportsDirectory);
				} catch (ClassNotFoundException e) {
					logger.log(LogService.LOG_ERROR, "Impossible to load class " + testBean.getId(), e);
				}
			}
		} catch (FileNotFoundException e) {
			logger.log(LogService.LOG_ERROR, null, e);
		}
	}

	public void shutdown() {
		stop = true;
		registry.removeTestRegistryListener(testListener);
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	public boolean isStopped() {
		return stop;
	}

	public boolean isRunning() {
		return running;
	}

}
