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

import static com.github.nfalco79.junit4osgi.runner.internal.SurefireConstants.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;

/**
 * This class generates test result as XML files compatible with Surefire.
 */
public class XMLReport {
	/**
	 * New line constant.
	 */
	private static final String NL = System.getProperty("line.separator", "\n");

	private Map<Integer, ReportInfo> map = new HashMap<Integer, XMLReport.ReportInfo>();
	private long totalTime;

	/**
	 * A test ends successfully.
	 *
	 * @param description
	 *            the test executed successfully.
	 */
	public void testCompleted(Description description) {
		ReportInfo info = map.get(description);
		info.endTime = System.currentTimeMillis();
	}

	/**
	 * A test a test will not be run.
	 *
	 * @param descriptor
	 *            the test descriptor
	 */
	public void testIgnored(Description description) {
		ReportInfo info = new ReportInfo();
		info.startTime = 0l;
		info.endTime = 0l;
		info.testDescription = description;
		map.put(description.hashCode(), info);
		info.type = FailureType.IGNORE;
	}

	private String formatNumber(double time) {
		return String.format(Locale.US, "%.3f", time);
	}

	/**
	 * A test throws an unexpected errors.
	 *
	 * @param test
	 *            the error cause
	 * @param out
	 *            the output messages printed during the test execution
	 * @param err
	 *            the error messages printed during the test execution
	 * @param log
	 *            the messages logged during the test execution
	 */
	public void testError(Failure failure, String out, String err, String log) {
		Description description = failure.getDescription();
		testCompleted(description);
		ReportInfo info = map.get(description);
		info.failure = failure;
		info.type = FailureType.ERROR;
		info.out = out;
		info.err = err;
		info.log = log;
	}

	/**
	 * A test fails.
	 *
	 * @param failure
	 *            the failing cause
	 * @param out
	 *            the output messages printed during the test execution
	 * @param err
	 *            the error messages printed during the test execution
	 * @param log
	 *            the messages logged during the test execution
	 */
	public void testFailed(Failure failure, String out, String err, String log) {
		Description description = failure.getDescription();
		testCompleted(description);
		ReportInfo info = map.get(description);
		info.failure = failure;
		info.type = FailureType.FAILURE;
		info.out = out;
		info.err = err;
		info.log = log;
	}

	/**
	 * Utility method writing failed and in error test result in the report.
	 *
	 * @param failure
	 *            the cause
	 * @param name
	 *            type of failure ("error" or "failure")
	 * @param out
	 *            the output messages printed during the test execution
	 * @param err
	 *            the error messages printed during the test execution
	 * @param log
	 *            the messages logged during the test execution
	 */
	private void writeTestProblems(Xpp3Dom element, Failure failure, String out, String err, String log) {
		Throwable exception = failure.getException();
		if (exception != null) {
			String message = failure.getMessage();
			if (message != null) {
				element.setAttribute("message", message);
			}

			element.setAttribute("type", exception.getClass().getName());
		}
		String stackTrace = failure.getTrace();
		if (stackTrace != null) {
			element.setValue(stackTrace);
		}

		addOutputStreamElement(element, out, SurefireConstants.TEST_STDOUT_ELEMENT);
		addOutputStreamElement(element, err, SurefireConstants.TEST_STDERR_ELEMENT);
		addOutputStreamElement(element, log, "log-service");
	}

	/**
	 * Generates the XML reports.
	 *
	 * @param test
	 *            the test
	 * @param result
	 *            the test result
	 * @param reportsDirectory
	 *            the directory in which reports are created.
	 * @throws FileNotFoundException
	 *             if reportsDirectory does not exists
	 */
	public void generateReport(TestBean test, File reportsDirectory) throws FileNotFoundException {
		File reportFile = new File(reportsDirectory, MessageFormat.format(DEFAULT_NAME, test.getName().replace(' ', '_')));

		File reportDir = reportFile.getParentFile();

		reportDir.mkdirs();

		PrintWriter writer = null;

		try {
			OutputStreamWriter osw = null;
			try {
				osw = new OutputStreamWriter(new FileOutputStream(reportFile), "UTF-8"); // NOSONAR close by writer
			} catch (UnsupportedEncodingException e) { // NOSONAR fallback
				osw = new OutputStreamWriter(new FileOutputStream(reportFile)); // NOSONAR close by writer
			}
			writer = new PrintWriter(new BufferedWriter(osw));
			writer.write(MessageFormat.format(XML_HEADER, osw.getEncoding()) + NL);

			int errorsCount = 0;
			int failuresCount = 0;
			int ignoredCount = 0;
			int runCount = 0;

			Xpp3Dom root = null;
			for (ReportInfo report : map.values()) {
				Description description = report.testDescription;

				if (description.isSuite()) {
					root = createTestSuiteElement(null, description);
					showProperties(root);
				} else if (description.isEmpty()) {
					root = createTestSuiteElement(null, description);
				} else if (description.isTest()) {
					runCount++;
					Xpp3Dom element = createTestElement(root, description);

					switch(report.type) {
					case ERROR:
						errorsCount++;
						element = createElement(element, TEST_FAILURE_ELEMENT);
						writeTestProblems(element, report.failure, report.out, report.err, report.log);
						break;
					case FAILURE:
						failuresCount++;
						element = createElement(element, TEST_ERROR_ELEMENT);
						writeTestProblems(element, report.failure, report.out, report.err, report.log);
						break;
					case IGNORE:
						ignoredCount++;
						element = createElement(element, TEST_SKIPED_ELEMENT);
						break;
					default:
						// it's a normal success test
						break;
					}
				} else {
					throw new IllegalStateException("Unexpected element description " + description);
				}
			}

			if (root != null) {
				root.setAttribute(SUITE_TESTS_ATTRIBUTE, String.valueOf(runCount));
				root.setAttribute(SUITE_FAILURES_ATTRIBUTE, String.valueOf(failuresCount));
				root.setAttribute(SUITE_ERRORS_ATTRIBUTE, String.valueOf(errorsCount));
				root.setAttribute(SUITE_IGNORED_ATTRIBUTE, String.valueOf(ignoredCount));
			}

			Xpp3DomWriter.write(new PrettyPrintXMLWriter(writer), root);
		} finally {
			IOUtil.close(writer);
		}
	}

	/**
	 * Creates a XML test case element.
	 *
	 * @param description
	 *            the test description
	 * @return the XML element describing the given test.
	 */
	private Xpp3Dom createTestElement(Xpp3Dom parent, Description description) {
		Xpp3Dom testCase = createElement(parent, "testcase");

		testCase.setAttribute(TEST_NAME_ATTRIBUTE, getReportName(description));
		testCase.setAttribute(TEST_CLASSNAME_ATTRIBUTE, description.getClassName());
		testCase.setAttribute(TEST_TIME_ATTRIBUTE, formatNumber(getTime(description)));

		return testCase;
	}

	private double getTime(Description description) {
		ReportInfo reportInfo = map.get(description.hashCode());
		return (reportInfo.endTime - reportInfo.startTime) / 1000d;
	}

	private String getReportName(Description description) {
		String displayName = description.getDisplayName();
		int parentesis = displayName.indexOf('(');
		if (parentesis > 0) {
			displayName = displayName.substring(0, parentesis);
		}
		return displayName;
	}

	/**
	 * Creates a XML test suite element.
	 *
	 * @param parent
	 *
	 * @param test
	 *            the test
	 * @return the XML element describing the given test suite.
	 */
	private Xpp3Dom createTestSuiteElement(Xpp3Dom parent, Description description) {
		Xpp3Dom testSuite = createElement(parent, SUITE_ELEMENT);

		testSuite.setAttribute(SUITE_NAME_ATTRIBUTE, getReportName(description));
		testSuite.setAttribute(SUITE_TIME_ATTRIBUTE, formatNumber(getTotalTime()));

		return testSuite;
	}

	private double getTotalTime() {
		return totalTime / 1000d;
	}

	/**
	 * Creates an XML element.
	 *
	 * @param element
	 *            the parent element
	 * @param name
	 *            the name of the element to create
	 * @return the resulting XML tree.
	 */
	private Xpp3Dom createElement(Xpp3Dom element, String name) {
		Xpp3Dom component = new Xpp3Dom(name);
		if (element != null) {
			element.addChild(component);
		}
		return component;
	}

	/**
	 * Adds system properties to the XML report. This method also adds installed
	 * bundles.
	 *
	 * @param testSuite
	 *            the XML element.
	 */
	private void showProperties(Xpp3Dom parent) {
		Xpp3Dom properties = createElement(parent, "properties");

		Properties systemProperties = System.getProperties();

		if (systemProperties != null) {
			Enumeration<?> propertyKeys = systemProperties.propertyNames();

			while (propertyKeys.hasMoreElements()) {
				String key = (String) propertyKeys.nextElement();

				String value = systemProperties.getProperty(key);
				if (value == null) {
					value = "null";
				}

				Xpp3Dom property = createElement(properties, "property");
				property.setAttribute("name", key);
				property.setAttribute("value", value);
			}
		}
	}

	/**
	 * Adds messages written during the test execution in the XML tree.
	 *
	 * @param stdOut
	 *            the messages
	 * @param name
	 *            the name of the stream (out, error, log)
	 * @param testCase
	 *            the XML tree
	 */
	private void addOutputStreamElement(Xpp3Dom parent, String stdOut, String name) {
		if (stdOut != null && stdOut.trim().length() > 0) {
			createElement(parent, name).setValue(stdOut);
		}
	}

	/**
	 * Callback called when a test starts.
	 *
	 * @param description
	 *            the test description.
	 */
	public void testStarted(Description description) {
		ReportInfo info = new ReportInfo();
		info.startTime = System.currentTimeMillis();
		info.testDescription = description;
		map.put(description.hashCode(), info);
	}

	public void testCompleted(Result result) {
		totalTime = result.getRunTime();
	}

	private enum FailureType {
		IGNORE, FAILURE, ERROR, NONE
	}

	private class ReportInfo {
		private String log;
		private String err;
		private String out;
		private Description testDescription;
		private long startTime;
		private long endTime;
		private Failure failure;
		private FailureType type = FailureType.NONE;
	}
}