package org.example;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ErrorTest {

	@Test
	public void errorTest() {
		System.out.print("test sysout errorTest");
		System.err.print("test syserr errorTest");
		throw new IllegalStateException("message");
	}

	@Test
	public void failureTest() {
		System.out.print("test sysout failureTest");
		System.err.print("test syserr failureTest");
		Assert.assertTrue("expected true", false);
	}

	@Test
	@Ignore
	public void ignoreTest() {
		System.out.print("test sysout ignoreTest");
		System.err.print("test syserr ignoreTest");
		// do nothing
	}

	@Test
	@Ignore
	public void ignoreTest2() {
		// do nothing
	}
}
