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

import static org.junit.Assert.fail;

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