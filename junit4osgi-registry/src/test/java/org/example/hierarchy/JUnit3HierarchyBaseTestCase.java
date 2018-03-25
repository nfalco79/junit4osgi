package org.example.hierarchy;

public class JUnit3HierarchyBaseTestCase extends AbstractJUnit3HierarchyTestCase {

	public void test_base() {
	}

	@Override
	void doTest() {
		System.out.println("base test case");
	}

}
