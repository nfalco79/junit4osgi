package org.example;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ErrorTest {

	@Test
	public void errorTest() {
		System.out.println("test sysout errorTest");
		System.err.println("test syserr errorTest");
		throw new IllegalStateException("message");
	}

	@Test
	public void failureTest() {
		System.out.println("test sysout failureTest");
		System.err.println("test syserr failureTest");
		Assert.assertTrue("expected true", false);
	}

	@Test
	@Ignore
	public void ignoreTest() {
		System.out.println("test sysout ignoreTest");
		System.err.println("test syserr ignoreTest");
		// do nothing
	}

	@Test
	@Ignore
	public void ignoreTest2() {
		// do nothing
	}
}
