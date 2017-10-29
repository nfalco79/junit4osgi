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

import org.junit.runner.notification.RunListener;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.runner.spi.TestRunnerNotifier;

/**
 * A safe notifier that wraps for a {@link TestRunnerNotifier} and logs any
 * exception raise by the wrapped notifier.
 *
 * @author nikolasfalco
 */
public class SafeTestRunnerNotifier implements TestRunnerNotifier {

	private static class NoNotifier implements TestRunnerNotifier {
		@Override
		public void stop() {
		}

		@Override
		public void start() {
		}

		@Override
		public RunListener getRunListener() {
			return null;
		}
	}

	private TestRunnerNotifier notifier;
	private LogService logger;

	public SafeTestRunnerNotifier(TestRunnerNotifier notifier, LogService logger) {
		this.notifier = notifier == null ? new NoNotifier() : notifier;
		this.logger = logger;
	}

	@Override
	public void start() {
		try {
			notifier.start();
		} catch (Exception e) {
			logger.log(LogService.LOG_INFO, "Notifier start error");
		}
	}

	@Override
	public void stop() {
		try {
			notifier.stop();
		} catch (Exception e) {
			logger.log(LogService.LOG_INFO, "Notifier stop error");
		}
	}

	@Override
	public RunListener getRunListener() {
		try {
			return notifier.getRunListener();
		} catch (Exception e) {
			logger.log(LogService.LOG_INFO, "Notifier error when return a RunListener");
		}
		return null;
	}

}
