package com.github.nfalco79.junit4osgi.gui.internal.runner.local;

import java.util.Set;

import com.github.nfalco79.junit4osgi.gui.internal.runner.TestExecutor;
import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;

public class LocalRunner implements TestExecutor {

	private TestRegistry registry;

	public LocalRunner(final TestRegistry registry) {
		if (registry == null) {
			throw new IllegalArgumentException("registry can not be null");
		}
		this.registry = registry;
	}

	@Override
	public String[] getTestsId() {
		Set<TestBean> tests = registry.getTests();
		String[] testsId = new String[tests.size()];

		int idx = 0;
		for (TestBean testBean : tests) {
			testsId[idx++] = testBean.getId();
		}
		return testsId;
	}

	@Override
	public void removeTestRegistryListener(TestRegistryChangeListener registryListener) {
		registry.removeTestRegistryListener(registryListener);
	}

	@Override
	public Set<TestBean> getTests(Set<String> testsId) {
		return registry.getTests(testsId.toArray(new String[testsId.size()]));
	}

}