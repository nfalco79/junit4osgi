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
package com.github.nfalco79.junit4osgi.registry.spi;

import java.net.URL;

import org.osgi.framework.Bundle;

public class TestBean {

	private String className;
	private Bundle bundle;

	public TestBean(Bundle bundle, String className) {
		if (bundle == null) {
			throw new NullPointerException("context is null");
		}
		if (className == null) {
			throw new NullPointerException("className is null");
		}

		URL entry = bundle.getEntry(className.replace('.', '/') + ".class");
		if (entry == null) {
			throw new IllegalArgumentException(className + " not found in bundle " + bundle.getSymbolicName());
		}
		this.bundle = bundle;
		this.className = className;
	}

	public String getName() {
		return className;
	}

	public Class<?> getTestClass() throws ClassNotFoundException {
		return bundle.loadClass(className);
	}

	public String getId() {
		return bundle.getSymbolicName() + '@' + className;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bundle == null) ? 0 : bundle.hashCode());
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TestBean other = (TestBean) obj;
		if (bundle == null) {
			if (other.bundle != null)
				return false;
		} else if (!bundle.equals(other.bundle))
			return false;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		return true;
	}

}
