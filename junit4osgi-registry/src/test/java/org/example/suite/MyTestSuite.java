package org.example.suite;

import org.example.JUnit3Test;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

public class MyTestSuite extends TestSuite {

	public static Test suite() {
		MyTestSuite suite = new MyTestSuite();
		suite.addTestSuite(JUnit3Test.class);

		return new TestSetup(suite);
	}
}
