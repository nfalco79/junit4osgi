package org.example;

import junit.framework.TestCase;

public class JUnit3Test extends TestCase {
	public final static String MESSAGE = "This is a simple junit3 test case";

	public void test_stdout() {
		System.out.println(MESSAGE);
	}

	public void test_stderr() {
		System.err.println(MESSAGE);
	}
}
