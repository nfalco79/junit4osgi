package org.example;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FlakyJUnit4Test {

    public static int test1Failures = 3;
    public static int test2Failures = 1;

    @Before
    public void setup() {
        System.out.println("setup");
    }

    @Test
    public void test1() {
        System.out.println("test1");
        if (test1Failures >= 0) {
            Assert.fail("test 1 failures " + test1Failures--);
        }
    }

    @Test
    public void test2() {
        System.out.println("test2");
        if (test2Failures >= 0) {
            test2Failures--;
            throw new NullPointerException();
        }
    }
}
