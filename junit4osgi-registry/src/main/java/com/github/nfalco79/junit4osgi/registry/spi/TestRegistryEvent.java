package com.github.nfalco79.junit4osgi.registry.spi;

public class TestRegistryEvent {

	public enum TestRegistryEventType {
		ADD, REMOVE
	}

	private TestRegistryEventType type;
	private TestBean test;

	public TestRegistryEvent(TestRegistryEventType type, TestBean test) {
		this.setType(type);
		this.setTest(test);
	}

	public TestRegistryEventType getType() {
		return type;
	}

	public void setType(TestRegistryEventType type) {
		this.type = type;
	}

	public TestBean getTest() {
		return test;
	}

	public void setTest(TestBean test) {
		this.test = test;
	}
}
