package org.example;
import org.junit.Assert;
import org.junit.Test;

public class CustomRunListenerTest {

    @Test
    public void failure() {
        Assert.fail("failure");
    }

    @Test
    public void error() {
        throw new UnsupportedOperationException();
    }
}
