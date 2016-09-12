package com.github.nfalco79.junit4osgi.registry;

public class TestEvent {

	public enum TestEventType {
		ADD, REMOVE
	}

	private TestEventType type;
	private TestBean test;

	public TestEvent(TestEventType type, TestBean test) {
		this.setType(type);
		this.setTest(test);
	}

	public TestEventType getType() {
		return type;
	}

	public void setType(TestEventType type) {
		this.type = type;
	}

	public TestBean getTest() {
		return test;
	}

	public void setTest(TestBean test) {
		this.test = test;
	}
}
