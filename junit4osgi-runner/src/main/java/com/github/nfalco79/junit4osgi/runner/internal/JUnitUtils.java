package com.github.nfalco79.junit4osgi.runner.internal;

import org.junit.Test;
import org.junit.runners.model.TestClass;

import junit.framework.TestCase;

/*package*/ final class JUnitUtils {

	private JUnitUtils() {
	}

	public static boolean hasTests(Class<?> testClass) {
		return testClass != null && (testClass.isAssignableFrom(TestCase.class)
				|| !new TestClass(testClass).getAnnotatedMethods(Test.class).isEmpty());
	}

}
