package org.example;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ExceptionOnClassMethodsTest {

	public static class MyRule implements TestRule {
		@Override
		public Statement apply(final Statement base, Description description) {
			return new Statement() {

				@Override
				public void evaluate() throws Throwable {
					base.evaluate();
					throw new NullPointerException("exception here");
				}
			};
		}
	}

	@ClassRule
	public static MyRule rule = new MyRule();

    @Test
    public void test() {
    }

}
