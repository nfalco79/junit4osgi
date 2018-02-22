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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * An implementation {@link RunListener} that gather all JUnit event and create
 * a complete {@link Report}.
 * <p>
 * This listener also deals to wrap standard output and error.
 *
 * @author Nikolas Falco
 */
public class ReportListener extends RunListener {

	public interface DequeValueMap<K, V> extends Map<K, Deque<V>> {
		boolean push(K key, V value);
		V peek(K key);
	}

	@SuppressWarnings("serial")
	private static class DequeValueMapImpl<K, V> extends HashMap<K, Deque<V>> implements DequeValueMap<K, V> {
		@Override
		public boolean push(K key, V value) {
			Deque<V> values = get(key);
			if (values == null) {
				values = new ArrayDeque<V>();
				put(key, values);
			}
			return values.add(value);
		}

		@Override
		public V peek(K key) {
			Deque<V> values = get(key);
			if (values != null) {
				return values.getLast();
			}
			return null;
		}
	}

	private static final String UTF_8 = "UTF-8";

	/**
	 * Backup of the {@link System#out} stream.
	 */
	private PrintStream outBackup = System.out; // NOSONAR

	/**
	 * Backup of the {@link System#err} stream.
	 */
	private PrintStream errBackup = System.err; // NOSONAR

	/**
	 * The output stream used during the test execution.
	 */
	private ByteArrayOutputStream out;

	/**
	 * The error stream used during the test execution.
	 */
	private ByteArrayOutputStream err;

	private DequeValueMap<Description, Report> executions = new DequeValueMapImpl<Description, Report>();
	private long startTime;
	private long totalTime;
	private int runCount;
	private Report root;

	/*
	 * (non-Javadoc)
	 * @see org.junit.runner.notification.RunListener#testIgnored(org.junit.runner.Description)
	 */
	@Override
	public void testIgnored(Description description) throws Exception {
		Report info = new Report(description);
		info.setElapsedTime(0d);
		info.markAsIgnored();
		info.setMessage(description.getAnnotation(Ignore.class).value());

		executions.push(description, info);
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runner.notification.RunListener#testFailure(org.junit.runner.notification.Failure)
	 */
	@Override
	public void testFailure(Failure failure) {
		long endTime = System.currentTimeMillis();

		Description description = failure.getDescription();
		Report info = executions.peek(description);
		info.setElapsedTime((endTime - startTime) / 1000d);
		info.setFailure(failure);
		info.setOut(toString(out));
		info.setErr(toString(err));
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runner.notification.RunListener#testAssumptionFailure(org.junit.runner.notification.Failure)
	 */
	@Override
	public void testAssumptionFailure(Failure failure) {
		testFailure(failure);
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runner.notification.RunListener#testStarted(org.junit.runner.Description)
	 */
	@Override
	public void testStarted(Description description) throws Exception {
		startTime = System.currentTimeMillis();

		Report info = new Report(description);

		err = new ByteArrayOutputStream();
		out = new ByteArrayOutputStream();
		System.setErr(new PrintStream(err));
		System.setOut(new PrintStream(out));

		executions.push(description, info);

		if (root == null) {
			root = info;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runner.notification.RunListener#testFinished(org.junit.runner.Description)
	 */
	@Override
	public void testFinished(Description description) throws Exception {
		long endTime = System.currentTimeMillis();
		System.setErr(errBackup);
		System.setOut(outBackup);

		Report info = executions.peek(description);
		info.setElapsedTime((endTime - startTime) / 1000d);
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runner.notification.RunListener#testRunStarted(org.junit.runner.Description)
	 */
	@Override
	public void testRunStarted(Description description) throws Exception {
		executions.clear();
		runCount = 0;
		totalTime = 0;

		root = new Report(description.getChildren().get(0));
		executions.push(root.getDescription(), root);
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runner.notification.RunListener#testRunFinished(org.junit.runner.Result)
	 */
	@Override
	public void testRunFinished(Result result) throws Exception {
		totalTime = result.getRunTime();
		runCount = result.getRunCount() + result.getIgnoreCount();
	}

	private static String toString(ByteArrayOutputStream out) {
		try {
			return out.toString(UTF_8);
		} catch (UnsupportedEncodingException e) {
			return out.toString();
		}
	}

	/**
	 * Returns if the given failure is due {@link AssertionError} or not.
	 *
	 * @param failure
	 *            as test result
	 * @return {@code true} if the failure is generated by a failed assertion,
	 *         {@code false} otherwise.
	 */
	public static boolean isFailure(Failure failure) {
		return failure != null && failure.getException() instanceof AssertionError;
	}

	/**
	 * Returns if the given failure is a general {@link Exception} runtime or
	 * not.
	 *
	 * @param failure
	 *            as test result
	 * @return {@code true} if the failure is raised by an exception,
	 *         {@code false} otherwise.
	 */
	public static boolean isError(Failure failure) {
		return failure != null && !(failure.getException() instanceof AssertionError);
	}

	/**
	 * Returns if the last execution of the given test was success or not.
	 *
	 * @param description
	 *            of the test
	 * @return {@code true} if last execution was success, {@code false}
	 *         otherwise.
	 */
	public boolean wasSuccess(Description description) {
		Report info = executions.peek(description);
		return info == null || info.getFailure() == null;
	}

	/**
	 * Gathers all execution test failed.
	 *
	 * @return an unmodifiable collection of test description.
	 */
	public Collection<Description> getFailures() {
		Collection<Description> failures = new LinkedList<Description>();
		for (Description test : executions.keySet()) {
			final Report lastRun = executions.peek(test);
			if (lastRun.getFailure() != null) {
				failures.add(lastRun.getFailure().getDescription());
			}
		}
		return Collections.unmodifiableCollection(failures);
	}

	/**
	 * Create a report from all JUnit events.
	 * <p>
	 * It builds a report that has a map where the key is the test description
	 * and the value is an ordered list of all executions result of that test.
	 *
	 * @return the generated report
	 */
	public Report getReport() {
		if (executions.isEmpty()) {
			return new Report(Description.createSuiteDescription("no test execution"));
		}

		Set<Entry<Description,Deque<Report>>> tests = executions.entrySet();

		Map<Description, Report> executionMap = new HashMap<Description, Report>(tests.size());

		for (Entry<Description, Deque<Report>> test : tests) {
			Deque<Report> runs = new ArrayDeque<Report>(test.getValue());
			Report report = null;
			// check if last run was success
			if (runs.peekLast().isSuccess()) {
				report = runs.pollLast();
			} else {
				// otherwise take the first failure
				report = runs.poll();
			}
			executionMap.put(test.getKey(), report);
			while (!runs.isEmpty()) {
				report.addRun(runs.poll());
			}
		}

		buildExecutionTree(executionMap, root.getDescription());

		root.setElapsedTime(totalTime / 1000d);
		root.setRunCount(runCount);
		return root;
	}

	private void buildExecutionTree(Map<Description, Report> executionMap, Description parent) {
		if (!executionMap.containsKey(parent)) {
			// in case of test suite
			executionMap.put(parent, new Report(parent));
		}

		for (Description child : parent.getChildren()) {
			if (!executionMap.containsKey(child)) {
				executionMap.put(child, new Report(child));
			}
			if (executionMap.containsKey(child)) {
				executionMap.get(parent).addChild(executionMap.get(child));
			}
			buildExecutionTree(executionMap, child);
		}
	}

}