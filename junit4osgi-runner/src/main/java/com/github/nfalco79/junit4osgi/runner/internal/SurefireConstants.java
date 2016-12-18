/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.nfalco79.junit4osgi.runner.internal;

/**
 * Define constants needed to create an XML surefire report.
 *
 * @author Nikolas Falco
 */
public final class SurefireConstants {
	private SurefireConstants() {
	}

	public static final String DEFAULT_CHARSET = "UTF-8";
	public static final String DEFAULT_NAME = "TEST-{0}.xml";

	/*
	 * XML constants
	 */
	public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"{0}\" ?>";
	public static final String CDATA_START = "<![CDATA[";
	public static final String CDATA_END = "]]>";

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
	public static final String TEST_ERROR_MESSAGE_ATTRIBUTE = "message";
	public static final String TEST_ERROR_TYPE_ATTRIBUTE = "type";
	public static final String TEST_FAILURE_ELEMENT = "failure";
	public static final String TEST_FAILURE_MESSAGE_ATTRIBUTE = "message";
	public static final String TEST_FAILURE_TYPE_ATTRIBUTE = "type";
	public static final String TEST_SKIPPED_ELEMENT = "skipped";
	public static final String TEST_NAME_ATTRIBUTE = "name";
	public static final String TEST_CLASSNAME_ATTRIBUTE = "classname";
	public static final String TEST_TIME_ATTRIBUTE = "time";

	/*
	 * Properties element and its children and attributes
	 */
	public static final String PROPERTIES_ELEMENT = "properties";
	public static final String PROPERTY_ELEMENT = "property";
	public static final String PROPERTY_NAME_ATTRIBUTE = "name";
	public static final String PROPERTY_VALUE_ATTRIBUTE = "value";
}