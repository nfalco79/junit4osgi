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

import org.junit.runner.notification.RunListener;

public interface TestRunnerNotifier {

	/**
	 * This method is invoked before the runner start the execution of the
	 * tests.
	 * <p>
	 * In case of infinite run this method is invoked each time a the test
	 * runner job is scheduled.
	 */
	void start();

	/**
	 * This method is invoked after the runner end the execution of the tests,
	 * also in case of exception.
	 * <p>
	 * In case of infinite run this method is invoked each time the scheduled
	 * test runner job ends.
	 */
	void stop();

	/**
	 * Gets the RunListener implementation used for each single test execution
	 * to be notified of its event.
	 *
	 * @return the implementation of {@link RunListener}
	 */
	RunListener getRunListener();

}