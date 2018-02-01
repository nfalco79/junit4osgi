package com.github.nfalco79.junit4osgi.runner.internal;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class RerunListenerWrapper extends RunListener {

	private RunListener delegate;

	public RerunListenerWrapper(RunListener delegate) {
		this.delegate = delegate;
	}

	@Override
	public void testRunStarted(Description description) throws Exception {
	}

	@Override
	public void testRunFinished(Result result) throws Exception {
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