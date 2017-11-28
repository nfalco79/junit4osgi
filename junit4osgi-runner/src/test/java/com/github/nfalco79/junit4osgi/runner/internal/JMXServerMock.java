package com.github.nfalco79.junit4osgi.runner.internal;

import org.junit.Assert;
import org.mockito.Mockito;

import com.github.nfalco79.junit4osgi.runner.internal.jmx.JMXServer;
import com.j256.simplejmx.server.JmxServer;

class JMXServerMock extends JMXServer {
	public JMXServerMock() {
		super();
		Assert.assertNotNull("JMX server not instantiated", jmxServer);
		Assert.assertTrue("jmx support disabled also if the server was instantiated", jmxSupportEnabled);
		jmxServer = Mockito.mock(JmxServer.class);
	}
}
