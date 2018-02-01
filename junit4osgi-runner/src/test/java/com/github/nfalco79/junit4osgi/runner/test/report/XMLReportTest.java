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
import static org.junit.Assert.*;

import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.example.ErrorTest;
import org.example.PropertyTest;
import org.example.SimpleSuiteTest;
import org.example.SimpleTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.JUnitCore;

import com.github.nfalco79.junit4osgi.runner.internal.Report;
import com.github.nfalco79.junit4osgi.runner.internal.ReportListener;
import com.github.nfalco79.junit4osgi.runner.internal.XMLReport;

public class XMLReportTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void simple_junit4() throws Exception {
		Report report = runTest(SimpleTestCase.class);

		String testName = SimpleTestCase.class.getName();

		// write test result
		File testFolder = folder.newFolder();
		new XMLReport(testFolder).generateReport(report);

		// check its name
		File xml = getReport(testFolder);
		assertEquals("Wrong file name", MessageFormat.format(DEFAULT_NAME, testName), xml.getName());

		// checks the content
		SurefireHelper helper = new SurefireHelper(xml);
		helper.verifySuite(testName, 3l, 0l, 0l, 0l);

		assertNotNull("No testcase element found", helper.hasTestCase());
		assertEquals("Unexpected testcase element found", 3, helper.countTestCase());

		helper.verifyTestCase(testName, "test_stdout", 0);
		helper.verifyTestCase(testName, "test_stderr", 0);
		helper.verifyTestCase(testName, "test_time", 0.5d);
	}

	@Test
	public void ignored() throws Exception {
		Report report = runTest(ErrorTest.class);

		String testName = ErrorTest.class.getName();

		// write test result
		File testFolder = folder.newFolder();
		new XMLReport(testFolder).generateReport(report);

		// checks the content
		File xml = getReport(testFolder);
		SurefireHelper helper = new SurefireHelper(xml);
		helper.verifySuite(testName, 5, 1, 1, 3);

		assertNotNull("No testcase element found", helper.hasTestCase());
		assertEquals("Unexpected testcase element found", 5, helper.countTestCase());

		helper.verifySkipedTestCase(testName, "ignoreTest");
		helper.verifySkipedTestCase(testName, "ignoreTest2");
		helper.verifySkipedTestCase(testName, "ignoreTest3", "reason message");
	}

	@Test
	public void failure() throws Exception {
		Report report = runTest(ErrorTest.class);

		String testName = ErrorTest.class.getName();

		// write test result
		File testFolder = folder.newFolder();
		new XMLReport(testFolder).generateReport(report);

		// checks the content
		File xml = getReport(testFolder);
		SurefireHelper helper = new SurefireHelper(xml);
		helper.verifySuite(testName, 5, 1, 1, 3);

		assertNotNull("No testcase element found", helper.hasTestCase());
		assertEquals("Unexpected testcase element found", 5, helper.countTestCase());

		Xpp3Dom testcase = helper.verifyTestCase(testName, "failureTest", 0d);
		helper.verifyFailure(testcase, AssertionError.class, "expected true");
		helper.verifyStdOutMessage(testcase, "test sysout failureTest");
		helper.verifyStdErrMessage(testcase, "test syserr failureTest");
	}

	@Test
	public void error() throws Exception {
		Report report = runTest(ErrorTest.class);

		String testName = ErrorTest.class.getName();

		// write test result
		File testFolder = folder.newFolder();
		new XMLReport(testFolder).generateReport(report);

		// checks the content
		File xml = getReport(testFolder);
		SurefireHelper helper = new SurefireHelper(xml);
		helper.verifySuite(testName, 5, 1, 1, 3);

		Xpp3Dom testcase = helper.verifyTestCase(testName, "errorTest", 0d);
		helper.verifyError(testcase, IllegalStateException.class, "message");
		helper.verifyStdOutMessage(testcase, "test sysout errorTest");
		helper.verifyStdErrMessage(testcase, "test syserr errorTest");
	}

	@Test
	public void property() throws Exception {
		String propertyKey = "my.test.property";
		String propertyValue = "my value!";
		System.setProperty(propertyKey, propertyValue);
		try {
			Report report = runTest(PropertyTest.class);

			// write test result
			File testFolder = folder.newFolder();
			new XMLReport(testFolder).generateReport(report);

			// checks the content
			File xml = getReport(testFolder);
			SurefireHelper helper = new SurefireHelper(xml);

			helper.verifyProperty(propertyKey, propertyValue);
		} finally {
			System.clearProperty(propertyKey);
		}
	}

	@Test
	public void test_suite() throws Exception {
		Report report = runTest(SimpleSuiteTest.class);

		String testName = SimpleSuiteTest.class.getName();

		// write test result
		File testFolder = folder.newFolder();
		new XMLReport(testFolder).generateReport(report);

		// checks the content
		File xml = getReport(testFolder);
		SurefireHelper helper = new SurefireHelper(xml);
		helper.verifySuite(testName, 8, 1, 1, 3, 0.5d);

		assertNotNull("No testcase element found", helper.hasTestCase());
		assertEquals("Unexpected testcase element found", 8, helper.countTestCase());

		testName = ErrorTest.class.getName();
		Xpp3Dom testcase = helper.verifyTestCase(testName, "failureTest", 0d);
		helper.verifyFailure(testcase, AssertionError.class, "expected true");
		helper.verifyStdOutMessage(testcase, "test sysout failureTest");
		helper.verifyStdErrMessage(testcase, "test syserr failureTest");

		testcase = helper.verifyTestCase(testName, "errorTest", 0d);
		helper.verifyError(testcase, IllegalStateException.class, "message");
		helper.verifyStdOutMessage(testcase, "test sysout errorTest");
		helper.verifyStdErrMessage(testcase, "test syserr errorTest");

		helper.verifySkipedTestCase(testName, "ignoreTest");
		helper.verifySkipedTestCase(testName, "ignoreTest2");
		helper.verifySkipedTestCase(testName, "ignoreTest3", "reason message");

		testName = SimpleTestCase.class.getName();
		helper.verifyTestCase(testName, "test_stdout", 0);
		helper.verifyTestCase(testName, "test_stderr", 0);
		helper.verifyTestCase(testName, "test_time", 0.5d);
	}

	private Report runTest(Class<?>... testClass) {
		JUnitCore core = new JUnitCore();
		ReportListener listener = new ReportListener();
		core.addListener(listener);
		core.run(testClass);
		return listener.getReport();
	}

	private File[] getReports(File testFolder) {
		return testFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("TEST-");
			}
		});
	}

	private File getReport(File testFolder) {
		File[] xmls = getReports(testFolder);

		// verify if the surefire report file exists
		assertEquals("Too or none surefire report found", 1, xmls.length);

		return xmls[0];
	}

}