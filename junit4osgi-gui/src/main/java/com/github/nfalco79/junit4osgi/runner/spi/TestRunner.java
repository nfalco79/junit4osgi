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
package com.github.nfalco79.junit4osgi.runner.spi;

import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;

/**
 * This is a runner that use a provided {@link TestRegistry} to run all
 * registered tests.
 *
 * @author nikolasfalco
 */
public interface TestRunner {

	/**
	 * Sets the {@link TestRegistry}.
	 *
	 * @param registry
	 *            from which takes the test to run
	 */
	void setRegistry(TestRegistry registry);

	/**
	 * Sets the {@link LogService} used to log
	 *
	 * @param logger
	 *            the log service
	 */
	void setLog(LogService logger);

	/**
	 * Performs the run of all test in the registry.
	 */
	void start();

	/**
	 * Performs the run only specify tests that matches in the registry.
	 *
	 * @param testIds a list of test identifier.
	 * @param reportsPath the folder where generate the surefire reports.
	 * @param notifier to be notified on test events.
	 * @see com.github.nfalco79.junit4osgi.registry.spi.TestBean#getId()
	 */
	void start(String[] testIds, String reportsPath, TestRunnerNotifier notifier);

	/**
	 * Marks the execution to be stopped.
	 * <p>
	 * The call to this method does not ensure to be immediate, it marks the job
	 * to be stopped but the effective stop will be performed after complete the
	 * current JUnit execution.
	 */
	void stop();

	/**
	 * Returns if the runner is marked to stop.
	 *
	 * @return {@code true} if is it marked, {@code false} otherwise
	 */
	boolean isStopped();

	/**
	 * Returns if it running the JUnit test case.
	 *
	 * @return {@code true} if it is runner the tests, {@code false} otherwise
	 */
	boolean isRunning();

}