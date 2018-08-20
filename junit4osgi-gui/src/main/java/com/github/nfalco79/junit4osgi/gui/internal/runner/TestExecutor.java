package com.github.nfalco79.junit4osgi.gui.internal.runner;

import java.util.Set;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;

public interface TestExecutor {

	String[] getTestsId();

	Set<TestBean> getTests(final Set<String> testsId);

	void removeTestRegistryListener(final TestRegistryChangeListener registryListener);
}
