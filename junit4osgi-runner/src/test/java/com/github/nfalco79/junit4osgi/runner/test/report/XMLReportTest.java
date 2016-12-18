package com.github.nfalco79.junit4osgi.runner.test.report;

import static com.github.nfalco79.junit4osgi.runner.internal.SurefireConstants.DEFAULT_NAME;
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

import com.github.nfalco79.junit4osgi.runner.internal.ReportListener;
import com.github.nfalco79.junit4osgi.runner.internal.XMLReport;

public class XMLReportTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void simple_junit4() throws Exception {
		XMLReport report = runTest(SimpleTestCase.class);

		String testName = SimpleTestCase.class.getName();

		// write test result
		File testFolder = folder.newFolder();
		report.generateReport(testFolder);

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
		XMLReport report = runTest(ErrorTest.class);

		String testName = ErrorTest.class.getName();

		// write test result
		File testFolder = folder.newFolder();
		report.generateReport(testFolder);

		// checks the content
		File xml = getReport(testFolder);
		SurefireHelper helper = new SurefireHelper(xml);
		helper.verifySuite(testName, 4, 1, 1, 2);

		assertNotNull("No testcase element found", helper.hasTestCase());
		assertEquals("Unexpected testcase element found", 4, helper.countTestCase());

		helper.verifySkipedTestCase(testName, "ignoreTest");
		helper.verifySkipedTestCase(testName, "ignoreTest2");
	}

	@Test
	public void failure() throws Exception {
		XMLReport report = runTest(ErrorTest.class);

		String testName = ErrorTest.class.getName();

		// write test result
		File testFolder = folder.newFolder();
		report.generateReport(testFolder);

		// checks the content
		File xml = getReport(testFolder);
		SurefireHelper helper = new SurefireHelper(xml);
		helper.verifySuite(testName, 4, 1, 1, 2);

		assertNotNull("No testcase element found", helper.hasTestCase());
		assertEquals("Unexpected testcase element found", 4, helper.countTestCase());

		Xpp3Dom testcase = helper.verifyTestCase(testName, "failureTest", 0d);
		helper.verifyFailure(testcase, AssertionError.class, "expected true");
		helper.verifyStdOutMessage(testcase, "test sysout failureTest");
		helper.verifyStdErrMessage(testcase, "test syserr failureTest");
	}

	@Test
	public void error() throws Exception {
		XMLReport report = runTest(ErrorTest.class);

		String testName = ErrorTest.class.getName();

		// write test result
		File testFolder = folder.newFolder();
		report.generateReport(testFolder);

		// checks the content
		File xml = getReport(testFolder);
		SurefireHelper helper = new SurefireHelper(xml);
		helper.verifySuite(testName, 4, 1, 1, 2);

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
			XMLReport report = runTest(PropertyTest.class);

			// write test result
			File testFolder = folder.newFolder();
			report.generateReport(testFolder);

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
		XMLReport report = runTest(SimpleSuiteTest.class);

		String testName = SimpleSuiteTest.class.getName();

		// write test result
		File testFolder = folder.newFolder();
		report.generateReport(testFolder);

		// checks the content
		File xml = getReport(testFolder);
		SurefireHelper helper = new SurefireHelper(xml);
		helper.verifySuite(testName, 7, 1, 1, 2, 0.5d);

		assertNotNull("No testcase element found", helper.hasTestCase());
		assertEquals("Unexpected testcase element found", 7, helper.countTestCase());

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

		testName = SimpleTestCase.class.getName();
		helper.verifyTestCase(testName, "test_stdout", 0);
		helper.verifyTestCase(testName, "test_stderr", 0);
		helper.verifyTestCase(testName, "test_time", 0.5d);
	}

	private XMLReport runTest(Class<?>... testClass) {
		JUnitCore core = new JUnitCore();
		XMLReport report = new XMLReport();
		core.addListener(new ReportListener(report));
		core.run(testClass);
		return report;
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