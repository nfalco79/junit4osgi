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

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * A wrapper of {@link RunListener} that delegates all JUnit events but remove
 * some initial events to handle multiple execution of the same test.
 *
 * @author nikolasfalco
 */
public class RerunListenerWrapper extends RunListener {

	private RunListener delegate;

	public RerunListenerWrapper(RunListener delegate) {
		this.delegate = delegate;
	}

	@Override
	public void testRunStarted(Description description) throws Exception {
		// avoid to reset global statistic
	}

	@Override
	public void testRunFinished(Result result) throws Exception {
		// avoid to reset global statistic
	}

	@Override
	public void testStarted(Description description) throws Exception {
		delegate.testStarted(description);
	}

	@Override
	public void testFinished(Description description) throws Exception {
		delegate.testFinished(description);
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		delegate.testFailure(failure);
	}

	@Override
	public void testAssumptionFailure(Failure failure) {
		delegate.testAssumptionFailure(failure);
	}

	@Override
	public void testIgnored(Description description) throws Exception {
		delegate.testIgnored(description);
	}

}