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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * This class generates test result as XML files compatible with Surefire.
 *
 * @author Nikolas Falco
 */
public class XMLReport {
	/**
	 * New line constant.
	 */
	private static final String NL = System.getProperty("line.separator", "\n");

	private Description root;
	private Map<Description, ReportInfo> map = new HashMap<Description, ReportInfo>();
	private long totalTime;
	private int errorsCount;
	private int failuresCount;
	private int ignoredCount;
	private int runCount;

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
	 * @param description
	 *            a description of the test
	 */
	public void testIgnored(Description description) {
		ReportInfo info = new ReportInfo();
		info.startTime = 0l;
		info.endTime = 0l;
		map.put(description, info);
		info.type = FailureType.IGNORE;
	}

	private String formatNumber(double time) {
		if (time == 0) {
			return "0";
		}
		return String.format(Locale.US, "%.3f", time);
	}

	/**
	 * A test throws an unexpected errors.
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
	public void testError(Failure failure, String out, String err, String log) {
		Description description = failure.getDescription();
		testCompleted(description);
		ReportInfo info = map.get(description);
		info.failure = failure;
		info.type = FailureType.ERROR;
		info.out = out;
		info.err = err;
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
	public void testFailure(Failure failure, String out, String err, String log) {
		Description description = failure.getDescription();
		testCompleted(description);
		ReportInfo info = map.get(description);
		info.failure = failure;
		info.type = FailureType.FAILURE;
		info.out = out;
		info.err = err;
	}

	/**
	 * Utility method writing failed and in error test result in the report.
	 *
	 * @param element
	 *            the DOM parent element under wrote the problem
	 * @param failure
	 *            the failing cause
	 * @param name
	 *            type of failure ("error" or "failure")
	 */
	private void writeTestProblems(Xpp3Dom element, Failure failure) {
		Throwable exception = failure.getException();
		if (exception != null) {
			String message = failure.getMessage();
			if (message != null) {
				element.setAttribute(TEST_FAILURE_MESSAGE_ATTRIBUTE, message);
			}

			element.setAttribute(TEST_FAILURE_TYPE_ATTRIBUTE, exception.getClass().getName());
		}
		String stackTrace = failure.getTrace();
		if (stackTrace != null) {
			element.setValue(stackTrace);
		}
	}

	/**
	 * Generates the XML reports.
	 *
	 * @param reportsDirectory
	 *            the directory in which reports are created.
	 * @throws IOException
	 *             if reportsDirectory does not exists or could not be create
	 *             the folder structure
	 */
	public void generateReport(File reportsDirectory) throws IOException {
		if (root == null || runCount == 0) {
			return;
		}

		Xpp3Dom dom = createDOM(null, root);
		dom.setAttribute(SUITE_TESTS_ATTRIBUTE, String.valueOf(runCount));
		dom.setAttribute(SUITE_FAILURES_ATTRIBUTE, String.valueOf(failuresCount));
		dom.setAttribute(SUITE_ERRORS_ATTRIBUTE, String.valueOf(errorsCount));
		dom.setAttribute(SUITE_IGNORED_ATTRIBUTE, String.valueOf(ignoredCount));

		File reportFile = new File(reportsDirectory, MessageFormat.format(DEFAULT_NAME, dom.getAttribute(SUITE_NAME_ATTRIBUTE).replace(' ', '_')));
		File reportDir = reportFile.getParentFile();
		FileUtils.forceMkdir(reportDir);

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

			Xpp3DomWriter.write(new PrettyPrintXMLWriter(writer), dom);
			writer.flush();
		} finally {
			IOUtil.close(writer);
		}
	}

	private Xpp3Dom createDOM(Xpp3Dom dom, Description description) {
		if ("null".equals(description.getClassName())) {
			// for unknown reason the root description it's void
			return createDOM(dom, description.getChildren().get(0));
		} else if (dom == null && description.isSuite()) {
			// suite test aggregate all test methods and ignore its class container
			dom = createTestSuiteElement(null, description);
			addProperties(dom);
		} else if (description.isEmpty()) {
			dom = createTestSuiteElement(dom, description);
		} else if (description.isTest()) {
			Xpp3Dom element = createTestElement(dom, description);

			ReportInfo report = map.get(description);
			if (report != null) {
				switch (report.type) {
				case ERROR:
					errorsCount++;
					addOutputStreamElement(element, report.out, SurefireConstants.TEST_STDOUT_ELEMENT);
					addOutputStreamElement(element, report.err, SurefireConstants.TEST_STDERR_ELEMENT);
					Xpp3Dom error = createElement(element, TEST_ERROR_ELEMENT);
					writeTestProblems(error, report.failure);
					break;
				case FAILURE:
					failuresCount++;
					addOutputStreamElement(element, report.out, SurefireConstants.TEST_STDOUT_ELEMENT);
					addOutputStreamElement(element, report.err, SurefireConstants.TEST_STDERR_ELEMENT);
					Xpp3Dom failure  = createElement(element, TEST_FAILURE_ELEMENT);
					writeTestProblems(failure, report.failure);
					break;
				case IGNORE:
					createElement(element, TEST_SKIPPED_ELEMENT);
					break;
				default:
					// it's a normal success test
					break;
				}
			}
		}

		for (Description child : description.getChildren()) {
			createDOM(dom, child);
		}

		return dom;
	}

	/**
	 * Creates a XML test case element.
	 *
	 * @param description
	 *            the test description
	 * @return the XML element describing the given test.
	 */
	private Xpp3Dom createTestElement(Xpp3Dom parent, Description description) {
		Xpp3Dom testCase = createElement(parent, TEST_ELEMENT);

		testCase.setAttribute(TEST_NAME_ATTRIBUTE, getReportName(description));
		testCase.setAttribute(TEST_CLASSNAME_ATTRIBUTE, description.getClassName());
		testCase.setAttribute(TEST_TIME_ATTRIBUTE, formatNumber(getTime(description)));

		return testCase;
	}

	private double getTime(Description description) {
		ReportInfo reportInfo = map.get(description);
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
	private void addProperties(Xpp3Dom parent) {
		Xpp3Dom properties = createElement(parent, PROPERTIES_ELEMENT);

		Properties systemProperties = System.getProperties();

		if (systemProperties != null) {
			Enumeration<?> propertyKeys = systemProperties.propertyNames();

			while (propertyKeys.hasMoreElements()) {
				String key = (String) propertyKeys.nextElement();

				String value = systemProperties.getProperty(key);
				if (value == null) {
					value = "null";
				}

				Xpp3Dom property = createElement(properties, PROPERTY_ELEMENT);
				property.setAttribute(PROPERTY_NAME_ATTRIBUTE, key);
				property.setAttribute(PROPERTY_VALUE_ATTRIBUTE, value);
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
			createElement(parent, name).setValue("<![CDATA[" + stdOut + "]]>");
		}
	}

	/**
	 * Callback called when a test starts.
	 *
	 * @param description
	 *            the test description.
	 */
	public void testStarted(Description description) {
		if (root == null) {
			root = description;
		}
		ReportInfo info = new ReportInfo();
		info.startTime = System.currentTimeMillis();
		map.put(description, info);
	}

	/**
	 * Callback called when a test starts.
	 *
	 * @param description
	 *            the test description.
	 */
	public void newTest(Description description) {
		map.clear();
		runCount = 0;
		totalTime = 0;
		failuresCount = 0;
		errorsCount = 0;
		root = description;
	}

	public void setResult(Result result) {
		totalTime = result.getRunTime();
		ignoredCount = result.getIgnoreCount();
		runCount = result.getRunCount() + ignoredCount;
	}

	private enum FailureType {
		IGNORE, FAILURE, ERROR, NONE
	}

	private static class ReportInfo {
		private String err;
		private String out;
		private long startTime;
		private long endTime;
		private Failure failure;
		private FailureType type = FailureType.NONE;
	}

}