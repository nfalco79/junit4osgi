/*
 * Copyright 2019 Nikolas Falco
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.github.nfalco79.junit4osgi.runner.internal.AntGlobPattern.IncludeExcludePattern;

public final class TestFilter {
    private final Set<IncludeExcludePattern> includes;
    private final Set<IncludeExcludePattern> excludes;

    public TestFilter(String includePatterns, String excludePatterns) {
        this.includes = getPatterns(includePatterns);
        this.excludes = getPatterns(excludePatterns);
    }

    private Set<IncludeExcludePattern> getPatterns(String value) {
        Set<String> patterns = parsePatterns(value);
        Set<IncludeExcludePattern> regexpPatterns = new LinkedHashSet<IncludeExcludePattern>(patterns.size());
        for (String pattern : patterns) {
            regexpPatterns.add(AntGlobPattern.parse(pattern));
        }
        return regexpPatterns;
    }

    private Set<String> parsePatterns(String patterns) {
        if (patterns != null && !"".equals(patterns)) {
            String[] pattern = patterns.split(",| +");
            if (pattern.length > 0) {
                return new LinkedHashSet<String>(Arrays.asList(pattern));
            }
        }
        return Collections.emptySet();
    }

    public boolean accept(String testName) {
        boolean matches = includes.isEmpty(); // by default accepts all

        Iterator<IncludeExcludePattern> include = includes.iterator();
        while (include.hasNext() && !matches) {
            matches = include.next().matches(testName);
        }

        Iterator<IncludeExcludePattern> exclude = excludes.iterator();
        while (exclude.hasNext() && matches) {
            if (exclude.next().matches(testName)) {
                matches = false;
                //jUnitRunner.logger.log(LogService.LOG_DEBUG, "Test class: " + testName + " excluded by exclude pattern");
            }
        }
        return matches;
    }
}