package com.github.nfalco79.junit4osgi.gui.internal.runner;

import java.util.Collections;
import java.util.Set;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;

public class DummyExecutor implements TestExecutor {

	@Override
	public String[] getTestsId() {
		return new String[0];
	}

	@Override
	public Set<TestBean> getTests(Set<String> testsId) {
		return Collections.emptySet();
	}

	@Override
	public void removeTestRegistryListener(TestRegistryChangeListener registryListener) {
	}

    @Override
    public <T> void runTest(T test) throws ClassNotFoundException {
    }

}
