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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.nfalco79.junit4osgi.runner.internal.AntGlobPattern.IncludeExcludePattern;

public class AntGlobPatternTest {

    @Test
    public void test_single_wildcard_pattern() {
        IncludeExcludePattern p = AntGlobPattern.parse("*Constants");
        assertTrue("the glob pattern does not matches", p.matches("com.acme.MyConstants"));
    }

    @Test
    public void test_wildcard_at_the_end() {
        IncludeExcludePattern p = AntGlobPattern.parse("com.acme*");
        assertTrue("the glob pattern does not matches", p.matches("com.acme.MyConstants"));
    }

    @Test
    public void test_question_mark_pattern() {
        IncludeExcludePattern p = AntGlobPattern.parse("*.??Constants");
        assertTrue("the glob pattern does not matches", p.matches("com.acme.MyConstants"));
    }

}