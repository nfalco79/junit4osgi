package org.example;

public final class PackageRetrieverUtils {

	private PackageRetrieverUtils() {
	}

	public static Class<?> getPackageTestClass() {
		return PackageTest.class;
	}
}
