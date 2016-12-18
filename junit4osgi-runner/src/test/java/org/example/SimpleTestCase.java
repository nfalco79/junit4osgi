package org.example;

import org.junit.Test;

public class SimpleTestCase {
	public final static String MESSAGE = "This is a simple test case";

	@Test
	public void test_stdout() {
		System.out.println(MESSAGE);
	}

	@Test
	public void test_stderr() {
		System.err.println(MESSAGE);
	}

	@Test
	public void test_time() throws InterruptedException {
		Thread.sleep(500l);
		System.err.println(MESSAGE);
	}

}