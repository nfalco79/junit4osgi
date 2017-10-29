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

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;

/**
 * Wrapper to render a TestBean on UI.
 */
public class TestRecord {
	private TestBean test;

	public TestRecord(final TestBean test) {
		this.test = test;
	}

	public TestBean getTest() {
		return test;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((test == null) ? 0 : test.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		return test == obj;
	}

	@Override
	public String toString() {
		return test == null ? "No test" : test.getName();
	}

}