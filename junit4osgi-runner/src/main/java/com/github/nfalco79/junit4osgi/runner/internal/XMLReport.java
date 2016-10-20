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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Locale;
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
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class XMLReport {
	/**
	 * New line constant.
	 */
	private static final String NL = System.getProperty("line.separator", "\n");

	private Xpp3Dom root = null;
	private int failuresCount;
	private int errorsCount;
	/**
	 * Time at the beginning of the test execution.
	 */
	private long startTime;

	/**
	 * Time at the end of the test execution.
	 */
	private long endTime;

	/**
	 * A test ends successfully.
	 *
	 * @param description
	 *            the test executed successfully.
	 */
	public void testCompleted(Description description) {
		endTime = System.currentTimeMillis();

		double runTime = (this.endTime - this.startTime) / 1000d;
		root.setAttribute("time", formatNumber(runTime));

		root = root.getParent();
	}

	/**
	 * A test a test will not be run.
	 *
	 * @param descriptor
	 *            the test descriptor
	 */
	public void testIgnored(Description description) {
		Xpp3Dom testCase = createElementByDescription(root, description);
		testCase.setAttribute("time", formatNumber(0d));

		createElement(testCase, "skipped");

		root = root.getParent();
	}

	private String formatNumber(double time) {
		return NumberFormat.getInstance(Locale.US).format(Double.toString(time));
	}

	private Xpp3Dom createElementByDescription(Xpp3Dom parent, Description description) {
		Xpp3Dom element = null;
		if (description.isSuite()) {
			element = createTestSuiteElement(parent, description);
			showProperties(element);
		} else if (description.isTest()) {
			element = createTestElement(parent, description);
		} else {
			throw new IllegalStateException("Unexpected element description " + description);
		}
		root = element;
		return element;
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
		endTime = System.currentTimeMillis();
		++errorsCount;

		writeTestProblems(failure, "error", out, err, log);
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
		endTime = System.currentTimeMillis();
		++failuresCount;

		writeTestProblems(failure, "failure", out, err, log);
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
	private void writeTestProblems(Failure failure, String name, String out, String err, String log) {
		Xpp3Dom element = createElement(root, name);

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

		addOutputStreamElement(out, "system-out");
		addOutputStreamElement(err, "system-err");
		addOutputStreamElement(log, "log-service");
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
	public void generateReport(TestBean test, Result result, File reportsDirectory) throws FileNotFoundException {
		File reportFile = new File(reportsDirectory, "TEST-" + test.getName().replace(' ', '_') + ".xml");

		File reportDir = reportFile.getParentFile();

		reportDir.mkdirs();

		PrintWriter writer = null;

		try {
			OutputStreamWriter osw = null;
			try {
				osw = new OutputStreamWriter(new FileOutputStream(reportFile), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				osw = new OutputStreamWriter(new FileOutputStream(reportFile));
			}
			writer = new PrintWriter(new BufferedWriter(osw));
			writer.write("<?xml version=\"1.0\" encoding=\"" + osw.getEncoding() + "\" ?>" + NL);

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

		testCase.setAttribute("name", getReportName(description));
		testCase.setAttribute("classname", description.getClassName());

		return testCase;
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
		Xpp3Dom testSuite = createElement(parent, "testsuite");

		testSuite.setAttribute("name", getReportName(description));

		return testSuite;
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
		if (element == null) {
			element = component;
		} else {
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
		Xpp3Dom properties = createElement(root, "properties");

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
	private void addOutputStreamElement(String stdOut, String name) {
		if (stdOut != null && stdOut.trim().length() > 0) {
			createElement(root, name).setValue(stdOut);
		}
	}

	/**
	 * Callback called when a test starts.
	 *
	 * @param description
	 *            the test description.
	 */
	public void testStarted(Description description) {
		startTime = System.currentTimeMillis();
		createElementByDescription(root, description);
	}

	public void testCompleted(Result result) {
		root.setAttribute("tests", String.valueOf(result.getRunCount() + result.getIgnoreCount()));
		root.setAttribute("failures", String.valueOf(failuresCount));
		root.setAttribute("errors", String.valueOf(errorsCount));
		root.setAttribute("ignored", String.valueOf(result.getIgnoreCount()));
		root.setAttribute("time", Double.toString(result.getRunTime() / 1000d));
	}

}
