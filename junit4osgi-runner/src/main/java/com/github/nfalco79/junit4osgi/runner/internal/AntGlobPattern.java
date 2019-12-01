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
package com.github.nfalco79.junit4osgi.runner.internal;

import java.util.regex.Pattern;

public class AntGlobPattern {

	public static class IncludeExcludePattern {
		private Pattern pattern;

		private IncludeExcludePattern(Pattern pattern) {
			this.pattern = pattern;
		}

		public boolean matches(String name) {
			return pattern.matcher(name).matches();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
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
			IncludeExcludePattern other = (IncludeExcludePattern) obj;
			if (pattern == null) {
				if (other.pattern != null)
					return false;
			} else if (!pattern.equals(other.pattern))
				return false;
			return true;
		}
	}

	private static Pattern translate(String antPattern) {
		StringBuilder regexp = new StringBuilder();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < antPattern.length(); i++) {
			char charAt = antPattern.charAt(i);
			if (charAt == '*' || charAt == '?') {
				if (sb.length() > 0) {
					regexp.append(Pattern.quote(sb.toString()));
					sb = new StringBuilder();
				}
				regexp.append(charAt == '*' ? ".*" : ".");
			} else {
				sb.append(charAt);
			}
		}
		if (sb.length() > 0) {
			regexp.append(Pattern.quote(sb.toString()));
		}
		return Pattern.compile(regexp.toString());
	}

	public static IncludeExcludePattern parse(String pattern) {
		return new IncludeExcludePattern(translate(pattern));
	}

}