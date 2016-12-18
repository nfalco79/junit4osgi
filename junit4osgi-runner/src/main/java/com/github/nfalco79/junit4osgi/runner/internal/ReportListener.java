/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
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

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * An implementation {@link RunListener} that delegate the events on a XML
 * report writer.
 *
 * <p>
 * This listener also deals to wrap standard output and error.
 *
 * @author Nikolas Falco
 */
public class ReportListener extends RunListener {

	private static final String UTF_8 = "UTF-8";

	/**
	 * The XML Report.
	 */
	private XMLReport report;

	/**
	 * Backup of the {@link System#out} stream.
	 */
	private PrintStream outBackup = System.out;

	/**
	 * Backup of the {@link System#err} stream.
	 */
	private PrintStream errBackup = System.err;

	/**
	 * The output stream used during the test execution.
	 */
	private ByteArrayOutputStream out;

	/**
	 * The error stream used during the test execution.
	 */
	private ByteArrayOutputStream err;

	/**
	 * Creates a ResultListener.
	 *
	 * @param report
	 *            the XML report
	 */
	public ReportListener(XMLReport report) {
		this.report = report;
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runner.notification.RunListener#testIgnored(org.junit.runner.Description)
	 */
	@Override
	public void testIgnored(Description description) throws Exception {
		report.testIgnored(description);
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runner.notification.RunListener#testFailure(org.junit.runner.notification.Failure)
	 */
	@Override
	public void testFailure(Failure failure) throws Exception {
		if (failure.getException() instanceof AssertionError) {
			report.testFailure(failure, out.toString(UTF_8), err.toString(UTF_8), null);
		} else {
			report.testError(failure, out.toString(UTF_8), err.toString(UTF_8), null);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runner.notification.RunListener#testAssumptionFailure(org.junit.runner.notification.Failure)
	 */
	@Override
	public void testAssumptionFailure(Failure failure) {
		try {
			report.testFailure(failure, out.toString(UTF_8), err.toString(UTF_8), null);
		} catch (UnsupportedEncodingException e) {
			report.testFailure(failure, out.toString(), err.toString(), null);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runner.notification.RunListener#testStarted(org.junit.runner.Description)
	 */
	@Override
	public void testStarted(Description description) throws Exception {
		report.testStarted(description);
		err = new ByteArrayOutputStream();
		out = new ByteArrayOutputStream();
		System.setErr(new ReportPrintStream(err, errBackup, true));
		System.setOut(new ReportPrintStream(out, outBackup, true));
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runner.notification.RunListener#testFinished(org.junit.runner.Description)
	 */
	@Override
	public void testFinished(Description description) throws Exception {
		report.testCompleted(description);
		System.setErr(errBackup);
		System.setOut(outBackup);
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runner.notification.RunListener#testRunStarted(org.junit.runner.Description)
	 */
	@Override
	public void testRunStarted(Description description) throws Exception {
		report.newTest(description);
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runner.notification.RunListener#testRunFinished(org.junit.runner.Result)
	 */
	@Override
	public void testRunFinished(Result result) throws Exception {
		report.setResult(result);
	}
}
