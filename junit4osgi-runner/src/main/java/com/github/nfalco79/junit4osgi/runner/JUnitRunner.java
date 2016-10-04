package com.github.nfalco79.junit4osgi.runner;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;

public class JUnitRunner {
	private TestRegistry registry;
	private boolean stop;
	private boolean running;
	private LogService logger;
	private RunListener listener;

	public void setRegistry(TestRegistry registry) {
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
		registry.addTestRegistryListener(new TestRegistryChangeListener() {

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
