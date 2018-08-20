/*
 * Copyright 2017 Nikolas Falco
 * Licensed under the Apache License, Version 2.0 (the
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
package com.github.nfalco79.junit4osgi.gui;

/**
 * Wrapper to render a TestBean on UI.
 */
public class TestModel {
	private String testId;
	private String testName;

	public TestModel(final String testId) {
		if (testId == null) {
			throw new IllegalArgumentException("test bean is null");
		}
		this.testId = testId;
		this.testName = testId.substring(testId.indexOf('@') + 1);
	}

	public String getTest() {
		return testId;
	}

	@Override
	public int hashCode() {
		return testId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TestModel) {
			TestModel tm = (TestModel) obj;
		    return testId.equals(tm.testId);
		}
		return false;
	}

	@Override
	public String toString() {
		return testName;
	}

}