package com.github.nfalco79.junit4osgi.runner.internal;

public final class SurefireConstants {
	private SurefireConstants() {
	}

	public static final String DEFAULT_CHARSET = "UTF-8";
	public static final String DEFAULT_NAME = "TEST-{0}.xml";

	/*
	 * XML constants
	 */
	public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"{0}\" ?>";
	/*
	 * Suite element and its children and attributes
	 */
	public static final String SUITE_ELEMENT = "testsuite";
	public static final String SUITE_NAME_ATTRIBUTE = "name";
	public static final String SUITE_TIME_ATTRIBUTE = "time";
	public static final String SUITE_TESTS_ATTRIBUTE = "tests";
	public static final String SUITE_FAILURES_ATTRIBUTE = "failures";
	public static final String SUITE_ERRORS_ATTRIBUTE = "errors";
	public static final String SUITE_IGNORED_ATTRIBUTE = "ignored";

	/*
	 * Test element and its children and attributes
	 */
	public static final String TEST_ELEMENT = "testcase";
	public static final String TEST_STDERR_ELEMENT = "system-err";
	public static final String TEST_STDOUT_ELEMENT = "system-out";
	public static final String TEST_ERROR_ELEMENT = "error";
	public static final String TEST_FAILURE_ELEMENT = "failure";
	public static final String TEST_SKIPED_ELEMENT = "skiped";
	public static final String TEST_NAME_ATTRIBUTE = "name";
	public static final String TEST_CLASSNAME_ATTRIBUTE = "classname";
	public static final String TEST_TIME_ATTRIBUTE = "time";

}