package org.example.suite;

import org.example.JUnit3Test;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

public class MyJUnit3SuiteTest extends TestSuite {

	public static Test suite() {
		MyJUnit3SuiteTest suite = new MyJUnit3SuiteTest();
		suite.addTestSuite(JUnit3Test.class);

		return new TestSetup(suite);
	}
}
