package com.github.nfalco79.junit4osgi.runner.test.report;

import static com.github.nfalco79.junit4osgi.runner.internal.SurefireConstants.DEFAULT_NAME;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;

import org.example.SimpleTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.JUnitCore;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.runner.internal.ReportListener;
import com.github.nfalco79.junit4osgi.runner.internal.XMLReport;

public class XMLReportTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void test_simple_test() throws Exception {
		XMLReport report = runTest(SimpleTestCase.class);

		TestBean testBean = mock(TestBean.class);
		when(testBean.getName()).thenReturn(SimpleTestCase.class.getName());

		// write test result
		File testFolder = folder.newFolder();
		report.generateReport(testBean, testFolder);
		File[] xmls = testFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("TEST-");
			}
		});
		assertEquals("Too or none surefire report found", 1, xmls.length);

		File xml = xmls[0];
		assertEquals("Wrong file name", MessageFormat.format(DEFAULT_NAME, testBean.getName()), xml.getName());

		SurefireHelper helper = new SurefireHelper(xml);
		helper.verifySuite(testBean.getName(), 2l, 0l, 0l, 0l);

		assertNotNull("No testcase element found", helper.hasTestCase());
		assertEquals("Unexpected testcase element found", 2, helper.countTestCase());

		helper.verifyTestCase(testBean.getName(), "test_stdout", 0);
		helper.verifyStdOutMessage("test_stdout", SimpleTestCase.MESSAGE);
		helper.verifyTestCase(testBean.getName(), "test_stderr", 0);
		helper.verifyStdErrMessage("test_stderr", SimpleTestCase.MESSAGE);
	}

	private XMLReport runTest(Class<?> testClass) {
		JUnitCore core = new JUnitCore();
		XMLReport report = new XMLReport();
		core.addListener(new ReportListener(report));
		core.run(testClass);
		return report;
	}
}
