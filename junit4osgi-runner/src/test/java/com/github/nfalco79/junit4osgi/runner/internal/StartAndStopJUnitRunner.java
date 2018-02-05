package com.github.nfalco79.junit4osgi.runner.internal;

import static org.junit.Assert.*;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.runner.spi.TestRunnerNotifier;

/*package*/ class StartAndStopJUnitRunner extends JUnitRunnerNoJMXServer {
	private final CountDownLatch latch = new CountDownLatch(1);

	@Override
	protected Runnable getSingleRunnable(File reportsDirectory, java.util.Queue<TestBean> tests, TestRunnerNotifier notifier) {
		final Runnable realRunnable = super.getSingleRunnable(reportsDirectory, tests, notifier);
		return new Runnable() {
			@Override
			public void run() {
				realRunnable.run();
				// needed to know when test has ran and stop the executor
				latch.countDown();
			}
		};
	}

	@Override
	public void stop() {
		waitExecution();
		super.stop();
	}

	public void waitExecution() {
		try {
			latch.await();
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
	}
}