package com.github.nfalco79.junit4osgi.runner.internal;

import org.example.SimpleTestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.JUnitCore;

public class ReportListenerTest {

	@Test
	public void verify_listener_returns_a_copy_of_internal_reports() {
		JUnitCore core = new JUnitCore();
		ReportListener listener = new ReportListener();
		core.addListener(listener);
		core.run(SimpleTestCase.class);

		Assert.assertEquals(3, listener.getReport().getChildren().size());
		Assert.assertEquals(3, listener.getReport().getChildren().size());
	}
}
