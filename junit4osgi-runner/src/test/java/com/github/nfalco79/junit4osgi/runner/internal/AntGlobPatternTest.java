package com.github.nfalco79.junit4osgi.runner.internal;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.nfalco79.junit4osgi.runner.internal.AntGlobPattern.IncludeExcludePattern;

public class AntGlobPatternTest {

    @Test
    public void test_single_wildcard_pattern() {
        IncludeExcludePattern p = AntGlobPattern.include("*Constants");
        assertTrue("the glob pattern does not matches", p.matches("com.acme.MyConstants"));
    }

    @Test
    public void test_wildcard_at_the_end() {
        IncludeExcludePattern p = AntGlobPattern.include("com.acme*");
        assertTrue("the glob pattern does not matches", p.matches("com.acme.MyConstants"));
    }

    @Test
    public void test_question_mark_pattern() {
        IncludeExcludePattern p = AntGlobPattern.exclude("*.??Constants");
        assertTrue("the glob pattern does not matches", p.matches("com.acme.MyConstants"));
    }

}