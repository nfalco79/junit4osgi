package com.github.nfalco79.junit4osgi.runner.internal;

import static org.junit.Assert.assertFalse;

import org.example.JUnit3Test;
import org.example.MainClassTest;
import org.junit.Test;

public class JUnitUtilsTest {

	@Test
	public void test_that_main_class_does_not_contains_tests() throws Exception {
		assertFalse("this main does not contains tests", JUnitUtils.hasTests(MainClassTest.class));
	}

	@Test
	public void test_that_junit3_class_has_tests() throws Exception {
		assertFalse("this class extends TestCase so it contains tests", JUnitUtils.hasTests(JUnit3Test.class));
	}

}