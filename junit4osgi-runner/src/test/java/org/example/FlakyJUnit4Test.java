package org.example;
import org.junit.Assert;
import org.junit.Test;

public class FlakyJUnit4Test {

    public static int test1Failures;
    public static int test2Failures;

    public static void reset() {
        test1Failures = 3;
        test2Failures = 1;
    }

    @Test
    public void test1() {
        System.out.print("test1");
        if (test1Failures >= 0) {
            Assert.fail("test 1 failures " + test1Failures--);
        }
    }

    @Test
    public void test2() {
        System.out.print("test2");
        if (test2Failures >= 0) {
            test2Failures--;
            throw new NullPointerException();
        }
    }
}
