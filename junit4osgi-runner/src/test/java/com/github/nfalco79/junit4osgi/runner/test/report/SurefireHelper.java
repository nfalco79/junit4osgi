package com.github.nfalco79.junit4osgi.runner.test.report;

import static com.github.nfalco79.junit4osgi.runner.internal.SurefireConstants.*;
import static org.hamcrest.Matchers.greaterThan;
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
		assertEquals("Unexpected ignored value", ignored, getLong(suite, SUITE_IGNORED_ATTRIBUTE));
		assertEquals("Unexpected failures value", failures, getLong(suite, SUITE_FAILURES_ATTRIBUTE));
		assertEquals("Unexpected errors value", errors, getLong(suite, SUITE_ERRORS_ATTRIBUTE));
		assertEquals("Unexpected tests value", tests, getLong(suite, SUITE_TESTS_ATTRIBUTE));
	}

	public boolean hasTestCase() {
		Xpp3Dom[] testcases = xml.getChildren(TEST_ELEMENT);
		return testcases != null && testcases.length > 0;
	}

	public int countTestCase() {
		return xml.getChildren(TEST_ELEMENT).length;
	}

	public void verifyTestCase(String className, String name, double time) {
		Xpp3Dom testcase = getTestCase(name);
		assertNotNull("Testcase " + name + "not found", testcase);
		assertEquals("Unexpected name value", name, testcase.getAttribute(TEST_NAME_ATTRIBUTE));
		assertEquals("Unexpected classname value", className, testcase.getAttribute(TEST_CLASSNAME_ATTRIBUTE));
		assertThat("Unexpected time value", getDouble(testcase, TEST_TIME_ATTRIBUTE), greaterThan(time));
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

	public void verifyStdOutMessage(String testName, String value) {
		Xpp3Dom testcase = getTestCase(testName);
		Xpp3Dom[] stdoutElement = testcase.getChildren(TEST_STDOUT_ELEMENT);
		assertNotNull("No stdout element found", stdoutElement);
		assertThat("Too many stdout elements", stdoutElement.length, greaterThan(1));

		Xpp3Dom stdout = testcase.getChild(TEST_STDOUT_ELEMENT);
		assertEquals("Wrong output", value, stdout.getValue());
	}

	public void verifyStdErrMessage(String testName, String value) {
		Xpp3Dom testcase = getTestCase(testName);
		Xpp3Dom[] stdoutElement = testcase.getChildren(TEST_STDERR_ELEMENT);
		assertNotNull("No stdout element found", stdoutElement);
		assertThat("Too many stdout elements", stdoutElement.length, greaterThan(1));

		Xpp3Dom stdout = testcase.getChild(TEST_STDOUT_ELEMENT);
		assertEquals("Wrong output", value, stdout.getValue());
	}

}