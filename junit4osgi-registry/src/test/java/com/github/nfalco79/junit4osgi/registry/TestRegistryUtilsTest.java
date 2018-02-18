/*
 * Copyright 2017 Nikolas Falco
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.nfalco79.junit4osgi.registry;

import static org.junit.Assert.*;

import org.example.AbstractTest;
import org.example.ITest;
import org.example.JUnit3Test;
import org.example.MainClassTest;
import org.example.PackageRetrieverUtils;
import org.example.SimpleTestCase;
import org.example.TooManyConstructors;
import org.example.suite.MyTestSuite;
import org.junit.Test;


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

	@Test
	public void consider_testsuite() throws Exception {
		assertTrue("this class is extebds junit.framework.TestSuite and must not be skipped", TestRegistryUtils.isValidTestClass(MyTestSuite.class));
	}

}