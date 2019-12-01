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

import static org.junit.Assert.*;

import org.example.ErrorTest;
import org.example.JUnit3Test;
import org.example.SimpleSuiteTest;
import org.junit.Test;

public class TestFilerTest {

    @Test
    public void test_includes() {
        TestFilter filter = new TestFilter("*Test", null);
        assertTrue(filter.accept(JUnit3Test.class.getName()));
    }

    @Test
    public void test_excludes() {
        TestFilter filter = new TestFilter(null, "*.?Unit*");
        assertFalse(filter.accept(JUnit3Test.class.getName()));
    }

    @Test
    public void test_both_includes_and_excludes() {
        TestFilter filter = new TestFilter("*Test", "*3*, *Simple*");
        assertTrue(filter.accept(ErrorTest.class.getName()));
        assertFalse(filter.accept(JUnit3Test.class.getName()));
        assertFalse(filter.accept(SimpleSuiteTest.class.getName()));
    }

    @Test
    public void test_separators() {
        TestFilter filter = new TestFilter("org.example.ErrorTest,org.example.Foo  org.example.JUnit3Test, *Binary*", null);
        assertTrue(filter.accept(ErrorTest.class.getName()));
        assertTrue(filter.accept(JUnit3Test.class.getName()));
        assertFalse(filter.accept(SimpleSuiteTest.class.getName()));
    }


    @Test
    public void by_default_includes_all_except_default() {
        JUnitRunner runner = new JUnitRunner();
        assertTrue(runner.accept(ErrorTest.class));
        assertTrue(runner.accept(JUnit3Test.class));
        assertTrue(runner.accept(SimpleSuiteTest.class));
        assertFalse(runner.accept(junit.extensions.TestSetup.class));
    }

}