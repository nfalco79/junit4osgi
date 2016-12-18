package com.github.nfalco79.junit4osgi.runner.internal;

import static org.junit.Assert.*;

import org.example.JUnit3Test;
import org.example.MainClassTest;
import org.example.SimpleTestCase;
import org.junit.Test;

public class JUnitUtilsTest {

	@Test
	public void test_that_main_class_does_not_contains_tests() throws Exception {
		assertFalse("this main does not contains tests", JUnitUtils.hasTests(MainClassTest.class));
	}

	@Test
	public void test_that_junit3_class_has_tests() throws Exception {
		assertTrue("this class extends TestCase so it contains tests for sure", JUnitUtils.hasTests(JUnit3Test.class));
	}

	@Test
	public void test_that_junit4_class_has_tests() throws Exception {
		assertTrue("this class has annotated method with @Test so has tests for sure", JUnitUtils.hasTests(SimpleTestCase.class));
	}
}