package com.github.nfalco79.junit4osgi.registry.spi;

import java.util.Set;

import org.osgi.framework.Bundle;

public interface TestRegistry {

	void registerTest(Bundle contributor, String testClass);

	void removeBundleTests(Bundle contributor);

	Set<TestBean> getTests();

	void addTestRegistryListener(TestRegistryChangeListener listener);

	void removeTestRegistryListener(TestRegistryChangeListener listener);

}