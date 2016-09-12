package com.github.nfalco79.junit4osgi.runner;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.TestBean;
import com.github.nfalco79.junit4osgi.registry.TestRegistryListener;
import com.github.nfalco79.junit4osgi.registry.TestSuiteRegistry;

public class JUnitRunner {
	private TestSuiteRegistry registry;
	private boolean stop;
	private boolean running;
	private LogService logger;
	private RunListener listener;

	public void setRegistry(TestSuiteRegistry registry) {
		this.registry = registry;
	}

	public void setLogger(LogService logger) {
		this.logger = logger;
	}

	public void setListener(RunListener listener) {
		this.listener = listener;
	}

	public void startup() {
		if (logger == null || listener == null || registry == null) {
			throw new IllegalStateException("One of logger, listener or test registry service component is missing");
		}

		stop = false;
		running = true;

		final Queue<TestBean> tests = new ConcurrentLinkedQueue<TestBean>(registry.getTests());
		registry.addTestListener(new TestRegistryListener() {

			@Override
			public void removed(TestBean testBean) {
				tests.remove(testBean);
			}

			@Override
			public void added(TestBean testBean) {
				tests.add(testBean);
			}
		});

		TestBean testBean = tests.poll();
		while (!isStopped() && testBean != null) {
			try {
				Class<?> testClass = testBean.getTestClass();
				Result result = JUnitCore.runClasses(testClass);
				// write test result
			} catch (ClassNotFoundException e) {
				logger.log(LogService.LOG_ERROR, "Impossible to load class " + testBean.getId(), e);
			}
		}

		running = false;
	}

	public void shutdown() {
		stop  = true;
	}

	public boolean isStopped() {
		return stop;
	}

	public boolean isRunning() {
		return running;
	}

}
