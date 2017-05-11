package com.github.nfalco79.junit4osgi.runner.internal.jmx;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;

import com.j256.simplejmx.server.JmxServer;

public class JMXServer {

	private JmxServer jmxServer;
	private boolean jmxSupportEnabled;

	public JMXServer() {
		try {
			Class.forName("com.j256.simplejmx.server.JmxServer");
			jmxSupportEnabled = true;
		} catch (ClassNotFoundException e) {
			getLog().log(Level.INFO, "simplejmx is not installed, JUnit registry and runner will not be present in the MBeanService");
		}
	}

	public void start() {
		if (jmxSupportEnabled) {
			jmxServer = new JmxServer(true);
			try {
				jmxServer.start();
			} catch (JMException e) {
				getLog().log(Level.WARNING, "Cannot start the server", e);
			}
		}
	}

	public void stop() {
		if (jmxSupportEnabled && jmxServer != null) {
			jmxServer.stop();
		}
	}

	public void register(Object obj) {
		if (jmxSupportEnabled && jmxServer != null && obj != null) {
			try {
				jmxServer.register(obj);
			} catch (JMException e) {
				getLog().log(Level.WARNING, "Cannot register the " + obj.toString() + " as mbean", e);
			}
		}
	}

	public void unregister(Object obj) {
		if (jmxSupportEnabled && jmxServer != null && obj != null) {
			jmxServer.unregister(obj);
		}
	}

	private Logger getLog() {
		return Logger.getLogger(JMXServer.class.getName());
	}
}