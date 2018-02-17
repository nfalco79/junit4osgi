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

import static com.github.nfalco79.junit4osgi.runner.internal.SurefireConstants.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

import com.github.nfalco79.junit4osgi.runner.internal.xml.util.Xpp3DomWriter;

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

	private int errorsCount;
	private int failuresCount;
	private int ignoredCount;

	private final File reportsDirectory;

	public XMLReport(File reportsDirectory) {
		if (reportsDirectory == null) {
			throw new NullPointerException("report directory is null");
		}
		this.reportsDirectory = reportsDirectory;
	}

	private String formatNumber(double time) {
		if (time == 0) {
			return "0";
		}
		return String.format(Locale.US, "%.3f", time);
	}

	/**
	 * Utility method writing error test result in the report.
	 *
	 * @param element
	 *            the DOM parent element under wrote the problem
	 * @param failure
	 *            the error cause
	 */
	private void writeTestError(Xpp3Dom element, Failure failure) {
		Throwable exception = failure.getException();
		if (exception != null) {
			String message = failure.getMessage();
			if (message != null) {
				element.setAttribute(TEST_ERROR_MESSAGE_ATTRIBUTE, message);
			}

			element.setAttribute(TEST_ERROR_TYPE_ATTRIBUTE, exception.getClass().getName());
		}
		String stackTrace = failure.getTrace();
		if (stackTrace != null) {
			element.setValue(stackTrace);
		}
	}

	/**
	 * Utility method writing failed test result in the report.
	 *
	 * @param element
	 *            the DOM parent element under wrote the problem
	 * @param failure
	 *            the failing cause
	 */
	private void writeTestFailure(Xpp3Dom element, Failure failure) {
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
	 * Utility method writing ignored test result in the report.
	 *
	 * @param element
	 *            the DOM parent element under wrote the problem
	 * @param failure
	 *            the failing cause
	 */
	private void writeTestSkipped(Xpp3Dom element, String message) {
		if (StringUtils.isNotEmpty(message)) {
			element.setAttribute(TEST_SKIPPED_MESSAGE_ATTRIBUTE, message);
		}
	}

	/**
	 * Generates the XML reports.
	 *
	 * @param report
	 *            generated as result of a {@link ReportListener}
	 * @throws IOException
	 *             if reportsDirectory does not exists or could not be create
	 *             the folder structure
	 */
	public void generateReport(Report report) throws IOException {
		if (report == null || report.getRunCount() == 0) {
			return;
		}

		FileUtils.forceMkdir(reportsDirectory);

		Xpp3Dom dom = createDOM(null, report);
		dom.setAttribute(SUITE_TESTS_ATTRIBUTE, String.valueOf(report.getRunCount()));
		dom.setAttribute(SUITE_FAILURES_ATTRIBUTE, String.valueOf(failuresCount));
		dom.setAttribute(SUITE_ERRORS_ATTRIBUTE, String.valueOf(errorsCount));
		dom.setAttribute(SUITE_SKIPPED_ATTRIBUTE, String.valueOf(ignoredCount));

		File reportFile = new File(reportsDirectory, MessageFormat.format(DEFAULT_NAME, dom.getAttribute(SUITE_NAME_ATTRIBUTE).replace(' ', '_')));

		Writer writer = null;
		try {
			try {
				writer = WriterFactory.newWriter(reportFile, WriterFactory.UTF_8);
			} catch (UnsupportedEncodingException e) {
				writer = WriterFactory.newPlatformWriter(reportFile);
			}
			writer.write(MessageFormat.format(XML_HEADER, WriterFactory.UTF_8) + NL);

			Xpp3DomWriter.write(writer, dom);
			writer.flush();
		} finally {
			IOUtil.close(writer);
		}
	}

	/**
	 * Creates the whole XML tree for the given report.
	 *
	 * @param dom
	 *            the DOM root element
	 * @param report
	 *            the test report
	 * @return the XML element describing the given test.
	 */
	private Xpp3Dom createDOM(Xpp3Dom dom, Report report) {
		Description description = report.getDescription();
		if (description.getTestClass() == null) {
			// for unknown reason the root description it's void
			return createDOM(dom, report.getChildren().get(0));
		} else if (dom == null && description.isSuite()) {
			// suite gather all test methods of all test classes ignoring their
			// class container
			dom = createTestSuiteElement(null, report);
			addProperties(dom);
		} else if (description.isEmpty()) {
			dom = createTestSuiteElement(dom, report);
		} else if (description.isTest()) {
			switch (report.getType()) {
			case ERROR:
				errorsCount++;
				createTestErrorElement(dom, report);
				break;
			case FAILURE:
				failuresCount++;
				createTestFailureElement(dom, report);
				break;
			case IGNORE:
				ignoredCount++;
				createTestIgnoreElement(dom, report);
				break;
			case SUCCESS:
				// it's a normal success test
				createTestSuccessElement(dom, report);
				break;
			}
		}

		for (Report child : report.getChildren()) {
			createDOM(dom, child);
		}

		return dom;
	}

	/**
	 * Creates an XML ignored test element.
	 *
	 * @param parent
	 *            the DOM parent element
	 * @param report
	 *            the test report
	 * @return the XML element describing the given test.
	 */
	private Xpp3Dom createTestIgnoreElement(Xpp3Dom parent, Report report) {
		final Xpp3Dom element = createTestElement(parent, report);

		Xpp3Dom skipped = createElement(element, TEST_SKIPPED_ELEMENT);
		writeTestSkipped(skipped, report.getMessage());

		return element;
	}

	/**
	 * Creates an XML success test element.
	 *
	 * @param parent
	 *            the DOM parent element
	 * @param report
	 *            the test report
	 * @return the XML element describing the given test.
	 */
	private Xpp3Dom createTestSuccessElement(Xpp3Dom parent, Report report) {
		final Xpp3Dom element = createTestElement(parent, report);

		for (Report run : report.getRuns()) {
			switch (run.getType()) {
			case ERROR:
				writeReruns(element, TEST_FLAKY_ERROR_ELEMENT, run);
				break;
			case FAILURE:
				writeReruns(element, TEST_FLAKY_FAILURE_ELEMENT, run);
				break;
			default:
				break;
			}
		}

		return element;
	}

	/**
	 * Creates an XML failure element.
	 *
	 * @param parent
	 *            the DOM parent element
	 * @param report
	 *            the test report
	 * @return the XML element describing the given test.
	 */
	private Xpp3Dom createTestFailureElement(Xpp3Dom parent, Report report) {
		final Xpp3Dom element = createTestElement(parent, report);

		addOutputStreamElement(element, report.getOut(), SurefireConstants.TEST_STDOUT_ELEMENT);
		addOutputStreamElement(element, report.getErr(), SurefireConstants.TEST_STDERR_ELEMENT);
		for (Report run : report.getRuns()) {
			writeReruns(element, TEST_FAILURE_RERUN_ELEMENT, run);
		}
		Xpp3Dom failure = createElement(element, TEST_FAILURE_ELEMENT);
		writeTestFailure(failure, report.getFailure());

		return element;
	}

	/**
	 * Creates an XML error element.
	 *
	 * @param parent
	 *            the DOM parent element
	 * @param report
	 *            the test report
	 * @return the XML element describing the given test.
	 */
	private Xpp3Dom createTestErrorElement(Xpp3Dom parent, Report report) {
		final Xpp3Dom element = createTestElement(parent, report);

		addOutputStreamElement(element, report.getOut(), SurefireConstants.TEST_STDOUT_ELEMENT);
		addOutputStreamElement(element, report.getErr(), SurefireConstants.TEST_STDERR_ELEMENT);
		for (Report run : report.getRuns()) {
			writeReruns(element, TEST_ERROR_RERUN_ELEMENT, run);
		}
		Xpp3Dom error = createElement(element, TEST_ERROR_ELEMENT);
		writeTestError(error, report.getFailure());

		return element;
	}

	private void writeReruns(Xpp3Dom parent, String elementName, Report report) {
		Xpp3Dom rerunError = createElement(parent, elementName);
		addOutputStreamElement(rerunError, report.getOut(), SurefireConstants.TEST_STDOUT_ELEMENT);
		addOutputStreamElement(rerunError, report.getErr(), SurefireConstants.TEST_STDERR_ELEMENT);
		writeTestError(rerunError, report.getFailure());
	}

	/**
	 * Creates a XML test case element.
	 *
	 * @param parent
	 *            the DOM parent element
	 * @param report
	 *            the test report
	 * @return the XML element describing the given test.
	 */
	private Xpp3Dom createTestElement(Xpp3Dom parent, Report report) {
		Xpp3Dom testCase = createElement(parent, TEST_ELEMENT);

		final Description description = report.getDescription();
		testCase.setAttribute(TEST_NAME_ATTRIBUTE, getReportName(description));
		testCase.setAttribute(TEST_CLASSNAME_ATTRIBUTE, description.getClassName());
		testCase.setAttribute(TEST_TIME_ATTRIBUTE, formatNumber(report.getElapsedTime()));

		return testCase;
	}

	protected String getReportName(Description description) {
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
	 *            the DOM parent element
	 * @param report
	 *            the test report
	 * @return the XML element describing the given test suite.
	 */
	private Xpp3Dom createTestSuiteElement(Xpp3Dom parent, Report report) {
		Xpp3Dom testSuite = createElement(parent, SUITE_ELEMENT);

		testSuite.setAttribute(SUITE_NAME_ATTRIBUTE, getReportName(report.getDescription()));
		testSuite.setAttribute(SUITE_TIME_ATTRIBUTE, formatNumber(report.getElapsedTime()));

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
		if (element != null) {
			element.addChild(component);
		}
		return component;
	}

	/**
	 * Adds system properties to the XML report.
	 *
	 * @param parent
	 *            element under that adds properties element
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
	 * @param parent
	 *            the parent element
	 * @param stdOut
	 *            the messages
	 * @param name
	 *            the name of the stream (out, error, log)
	 */
	protected void addOutputStreamElement(Xpp3Dom parent, String stdOut, String name) {
		if (stdOut != null && stdOut.trim().length() > 0) {
			createElement(parent, name).setValue("<![CDATA[" + stdOut + "]]>");
		}
	}

}