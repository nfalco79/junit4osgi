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
package com.github.nfalco79.junit4osgi.runner.test.report;

import static com.github.nfalco79.junit4osgi.runner.internal.SurefireConstants.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class SurefireHelper {

	private Xpp3Dom xml;

	public SurefireHelper(File report) throws XmlPullParserException, IOException {
		FileReader reader = new FileReader(report);
		try {
			xml = Xpp3DomBuilder.build(reader);
		} finally {
			IOUtil.close(reader);
		}
	}

	public void verifySuite(String className, long tests, long failures, long errors, long ignored) {
		Xpp3Dom suite = xml;
		assertNotNull("No suite element found", suite);
		assertEquals("No suite element found", SUITE_ELEMENT, suite.getName());
		assertEquals("Unexpected suite name value", className, suite.getAttribute(SUITE_NAME_ATTRIBUTE));
		assertEquals("Unexpected ignored value", ignored, getLong(suite, SUITE_SKIPPED_ATTRIBUTE));
		assertEquals("Unexpected failures value", failures, getLong(suite, SUITE_FAILURES_ATTRIBUTE));
		assertEquals("Unexpected errors value", errors, getLong(suite, SUITE_ERRORS_ATTRIBUTE));
		assertEquals("Unexpected tests value", tests, getLong(suite, SUITE_TESTS_ATTRIBUTE));
	}

	public void verifySuite(String className, long tests, long failures, long errors, long ignored, double minElapsedSeconds) {
		verifySuite(className, tests, failures, errors, ignored);
		Xpp3Dom suite = xml;
		assertThat("Unexpected " + TEST_TIME_ATTRIBUTE + " value", getDouble(suite, TEST_TIME_ATTRIBUTE), greaterThanOrEqualTo(minElapsedSeconds));
	}

	public boolean hasTestCase() {
		Xpp3Dom[] testcases = xml.getChildren(TEST_ELEMENT);
		return testcases != null && testcases.length > 0;
	}

	public int countTestCase() {
		return xml.getChildren(TEST_ELEMENT).length;
	}

	public Xpp3Dom verifyTestCase(String className, String name, double minElapsedSeconds) {
		Xpp3Dom testcase = getTestCase(name);
		assertNotNull("Testcase " + name + " not found", testcase);
		assertEquals("Unexpected " + TEST_NAME_ATTRIBUTE + " value", name, testcase.getAttribute(TEST_NAME_ATTRIBUTE));
		assertEquals("Unexpected " + TEST_CLASSNAME_ATTRIBUTE + " value", className, testcase.getAttribute(TEST_CLASSNAME_ATTRIBUTE));
		assertThat("Unexpected " + TEST_TIME_ATTRIBUTE + " value", getDouble(testcase, TEST_TIME_ATTRIBUTE), greaterThanOrEqualTo(minElapsedSeconds));

		return testcase;
	}

	public Xpp3Dom verifyError(Xpp3Dom element, Class<? extends Throwable> cause, String message) {
		Xpp3Dom[] errors = element.getChildren(TEST_ERROR_ELEMENT);
		assertThat("Too or no " + TEST_ERROR_ELEMENT + " element found", errors.length, greaterThanOrEqualTo(1));
		Xpp3Dom error = errors[0];
		assertEquals("Unexpected " + TEST_ERROR_TYPE_ATTRIBUTE + " value", cause.getName(), error.getAttribute(TEST_ERROR_TYPE_ATTRIBUTE));
		assertEquals("Unexpected " + TEST_ERROR_MESSAGE_ATTRIBUTE + " value", message, error.getAttribute(TEST_ERROR_MESSAGE_ATTRIBUTE));

		return error;
	}

	public Xpp3Dom verifyFailure(Xpp3Dom element, Class<? extends Throwable> cause, String message) {
		Xpp3Dom[] failures = element.getChildren(TEST_FAILURE_ELEMENT);
		assertThat("Too or no " + TEST_FAILURE_ELEMENT + " element found", failures.length, greaterThanOrEqualTo(1));
		Xpp3Dom failure = failures[0];
		assertEquals("Unexpected " + TEST_FAILURE_TYPE_ATTRIBUTE + " value", cause.getName(), failure.getAttribute(TEST_FAILURE_TYPE_ATTRIBUTE));
		assertEquals("Unexpected " + TEST_FAILURE_MESSAGE_ATTRIBUTE + " value", message, failure.getAttribute(TEST_FAILURE_MESSAGE_ATTRIBUTE));

		return failure;
	}

	public Xpp3Dom[] verifyFlakyFailure(Xpp3Dom element, Class<? extends Throwable> cause, int runs, String...messages) {
		Xpp3Dom[] failures = element.getChildren(TEST_FLAKY_FAILURE_ELEMENT);
		assertThat("Too or no " + TEST_FLAKY_FAILURE_ELEMENT + " element found", failures.length, greaterThanOrEqualTo(runs));

		for (int i = 0; i < messages.length; i++) {
			Xpp3Dom failure = failures[i];
			assertEquals("Unexpected " + TEST_FAILURE_TYPE_ATTRIBUTE + " value", cause.getName(), failure.getAttribute(TEST_FAILURE_TYPE_ATTRIBUTE));
			assertThat("Unexpected " + TEST_FAILURE_MESSAGE_ATTRIBUTE + " value", failure.getAttribute(TEST_FAILURE_MESSAGE_ATTRIBUTE), isOneOf(messages));
		}
		return failures;
	}

	public Xpp3Dom[] verifyFlakyError(Xpp3Dom element, Class<? extends Throwable> cause, int runs, String...messages) {
		Xpp3Dom[] failures = element.getChildren(TEST_FLAKY_ERROR_ELEMENT);
		assertThat("Too or no " + TEST_FLAKY_ERROR_ELEMENT + " element found", failures.length, greaterThanOrEqualTo(runs));

		for (int i = 0; i < messages.length; i++) {
			Xpp3Dom failure = failures[i];
			assertEquals("Unexpected " + TEST_ERROR_TYPE_ATTRIBUTE + " value", cause.getName(), failure.getAttribute(TEST_ERROR_TYPE_ATTRIBUTE));
			assertThat("Unexpected " + TEST_ERROR_MESSAGE_ATTRIBUTE + " value", failure.getAttribute(TEST_ERROR_MESSAGE_ATTRIBUTE), isOneOf(messages));
		}
		return failures;
	}

	public Xpp3Dom verifySkipedTestCase(String className, String name) {
		return verifySkipedTestCase(className, name, null);
	}

	public Xpp3Dom verifySkipedTestCase(String className, String name, String message) {
		Xpp3Dom testcase = verifyTestCase(className, name, 0);

		Xpp3Dom[] skips = testcase.getChildren(TEST_SKIPPED_ELEMENT);
		assertThat("Too or no " + TEST_SKIPPED_ELEMENT + " element found", skips.length, equalTo(1));
		Xpp3Dom skipped = skips[0];
		assertThat("Unexpected attributes on " + TEST_SKIPPED_ELEMENT + " element", skipped.getAttributeNames(), arrayWithSize(message == null ? 0 : 1));
		if (message != null) {
			assertThat("Unexpected message on " + TEST_SKIPPED_ELEMENT + " element", skipped.getAttribute(TEST_SKIPPED_MESSAGE_ATTRIBUTE), is(message));
		}
		Xpp3Dom[] children = testcase.getChildren();
		assertEquals("Unexpected element found", 1, children.length);

		return testcase;
	}

	private Xpp3Dom getTestCase(String name) {
		Xpp3Dom[] testcases = xml.getChildren(TEST_ELEMENT);
		assertNotNull("No testcase element found", testcases);
		Xpp3Dom result = null;
		for (Xpp3Dom testcase : testcases) {
			if (name.equals(testcase.getAttribute(TEST_NAME_ATTRIBUTE))) {
				result = testcase;
			}
		}
		return result;
	}

	private long getLong(Xpp3Dom element, String attributeName) {
		return Long.parseLong(element.getAttribute(attributeName));
	}

	private double getDouble(Xpp3Dom element, String attributeName) {
		return Double.parseDouble(element.getAttribute(attributeName));
	}

	public void verifyStdOutMessage(Xpp3Dom element, String value) {
		Xpp3Dom[] stdoutElement = element.getChildren(TEST_STDOUT_ELEMENT);
		assertThat("No " + TEST_STDOUT_ELEMENT + " element found", stdoutElement, not(arrayWithSize(0)));
		assertThat("Too many "+ TEST_STDOUT_ELEMENT + " elements", stdoutElement, arrayWithSize(1));

		Xpp3Dom stdout = stdoutElement[0];
		assertEquals("Wrong output", value, unwrapCData(stdout.getValue()));
	}

	public void verifyStdErrMessage(Xpp3Dom element, String value) {
		Xpp3Dom[] stderrElement = element.getChildren(TEST_STDERR_ELEMENT);
		assertNotNull("No " + TEST_STDERR_ELEMENT + " element found", stderrElement);
		assertThat("Too many " + TEST_STDERR_ELEMENT + " elements", stderrElement, arrayWithSize(1));

		Xpp3Dom stderr = stderrElement[0];
		assertEquals("Wrong output", value, unwrapCData(stderr.getValue()));
	}

	public void verifyProperty(String name, String value) {
		Xpp3Dom suite = xml;
		Xpp3Dom propertiesEl = suite.getChild(PROPERTIES_ELEMENT);
		assertNotNull("No " + PROPERTIES_ELEMENT + " element found", propertiesEl);
		Xpp3Dom[] properties = propertiesEl.getChildren(PROPERTY_ELEMENT);
		assertThat("No "+ PROPERTY_ELEMENT + " elements", properties, arrayWithSize(greaterThan(1)));

		for (Xpp3Dom property : properties) {
			String propertyKey = property.getAttribute(PROPERTY_NAME_ATTRIBUTE);
			if (name.equals(propertyKey)) {
				String propertyValue = property.getAttribute(PROPERTY_VALUE_ATTRIBUTE);
				assertEquals("Property '" + propertyKey + "' value is different", value, propertyValue);
				return;
			}
		}
		fail("Property '" + name + "' not found");
	}

	private String unwrapCData(final String text) {
		String unwrap = text;
		if (unwrap.startsWith(CDATA_START)) {
			unwrap = unwrap.substring(CDATA_START.length(), unwrap.length() - CDATA_END.length());
		}
		return unwrap;
	}
}