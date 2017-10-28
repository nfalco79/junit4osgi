package com.github.nfalco79.junit4osgi.registry;

import static org.junit.Assert.*;

import org.example.AbstractTest;
import org.example.ITest;
import org.example.JUnit3Test;
import org.example.MainClassTest;
import org.example.PackageRetrieverUtils;
import org.example.SimpleTestCase;
import org.example.TooManyConstructors;
import org.junit.Test;

import com.github.nfalco79.junit4osgi.registry.TestRegistryUtils;


public class TestRegistryUtilsTest {

	@Test
	public void main_class_does_not_contains_tests() throws Exception {
		assertFalse("this main does not contains tests", TestRegistryUtils.hasTests(MainClassTest.class));
	}

	@Test
	public void junit3_class_has_tests() throws Exception {
		assertTrue("this class extends TestCase so it contains tests for sure", TestRegistryUtils.hasTests(JUnit3Test.class));
	}

	@Test
	public void junit4_class_has_tests() throws Exception {
		assertTrue("this class has annotated method with @Test so has tests for sure", TestRegistryUtils.hasTests(SimpleTestCase.class));
	}

	@Test
	public void abstract_classes_are_skipped() throws Exception {
		assertFalse("this class is abstract and must be skipped", TestRegistryUtils.isValid(AbstractTest.class));
	}

	@Test
	public void interface_classes_are_skipped() throws Exception {
		assertFalse("this class is an interface and must be skipped", TestRegistryUtils.isValid(ITest.class));
	}

	@Test
	public void too_many_constructor_classes_are_skipped_without_exception() throws Exception {
		assertFalse("this class has too constructors and must be skipped without raise exceptions", TestRegistryUtils.hasTests(TooManyConstructors.class));
	}

	@Test
	public void no_constructor_not_available() throws Exception {
		assertFalse("this class has not public visibility and must be skipped", TestRegistryUtils.isValid(PackageRetrieverUtils.getPackageTestClass()));
	}

}