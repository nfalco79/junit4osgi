package org.example.hierarchy;

import junit.framework.TestCase;

public abstract class AbstractHierarchyTestCase extends TestCase {

	@Override
	protected void setUp() throws Exception {
	}

	public void test_abstract() {
		doTest();
	}


	abstract void doTest();
}