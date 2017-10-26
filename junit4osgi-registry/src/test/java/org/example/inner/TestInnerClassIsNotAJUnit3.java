package org.example.inner;

import junit.framework.TestCase;

public class TestInnerClassIsNotAJUnit3 extends TestCase {

	public void test_outer() {
	}

	public static class XClass {

		public void test_inner() {
			fail("this is not a junit3 class");
		}

	}

}