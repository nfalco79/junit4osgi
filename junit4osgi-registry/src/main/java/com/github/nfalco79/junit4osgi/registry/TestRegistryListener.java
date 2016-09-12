package com.github.nfalco79.junit4osgi.registry;

public interface TestRegistryListener {

	void removed(TestBean testBean);

	void added(TestBean testBean);
}
