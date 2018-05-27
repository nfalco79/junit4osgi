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

import java.lang.reflect.Modifier;

/**
 * Utility class
 *
 * @author Nikolas Falco
 */
import org.junit.Test;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.model.TestClass;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public final class TestRegistryUtils {

	private TestRegistryUtils() {
	}

	/**
	 * Returns if the given class is a valid JUnit 3/4 class and contains tests.
	 *
	 * @param testClass to analyse.
	 * @return {@code true} is a JUnit class that contains test method,
	 *         {@code false} otherwise.
	 */
	public static boolean hasTests(Class<?> testClass) {
		try {
			return testClass != null && (isJUnit3(testClass) || isJUnit4(testClass));
		} catch (Exception e) {
			// catch exception raised by TestClass
			return false;
		}
	}

	private static boolean isJUnit4(Class<?> testClass) {
		return !new TestClass(testClass).getAnnotatedMethods(Test.class).isEmpty() || testClass.getAnnotation(SuiteClasses.class) != null;
	}

	private static boolean isJUnit3(Class<?> testClass) {
		return junit.framework.Test.class.isAssignableFrom(testClass) && testClass != TestCase.class && testClass != TestSuite.class;
	}

	/**
	 * Returns if the given class is an normal declared java class.
	 *
	 * @param testClass to analyse.
	 * @return {@code true} if the given class is a public, not abstract, not
	 *         enumeration or not an interface, {@code false} otherwise.
	 */
	public static boolean isValid(Class<?> testClass) {
		final int modifiers = testClass.getModifiers();
		return Modifier.isPublic(modifiers) && !Modifier.isAbstract(modifiers) && !testClass.isInterface() && !testClass.isEnum();
	}


	/**
	 * Returns if the given class is a valid JUnit 3/4 class and contains tests.
	 *
	 * @param testClass to analyse.
	 * @return {@code true} if the given class is a JUnit3/4 public class, not abstract, not
	 *         enumeration or not an interface and that contains test methods, {@code false} otherwise.
	 */
	public static boolean isValidTestClass(Class<?> testClass) {
		return isValid(testClass) && hasTests(testClass);
	}
}