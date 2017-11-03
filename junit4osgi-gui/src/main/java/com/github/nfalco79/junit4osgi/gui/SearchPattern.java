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

public class SearchPattern {

	private String[] patterns;

	public SearchPattern(final String pattern) {
		if (pattern != null && !"".equals(pattern.trim())) {
			patterns = pattern.split(" ");

			for (int i = 0; i < patterns.length; i++) {
			    patterns[i] = patterns[i].toLowerCase();
			}
		}
	}

	public boolean matches(final String text) {
		if (patterns == null) {
			return true;
		}

		if (text == null || "".equals(text.trim())) {
			return false;
		}

		for (String pattern : patterns) {
			if (!text.contains(pattern)) {
				return false;
			}
		}
		return true;
	}
}
