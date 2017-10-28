package org.example;

import org.junit.Test;

public final class PackageRetrieverUtils {

	private PackageRetrieverUtils() {
	}

	public static Class<?> getPackageTestClass() {
		return PackageTest.class;
	}

	/* package*/ static class PackageTest {

		@Test
		public void a_test() {
		}

	}
}
