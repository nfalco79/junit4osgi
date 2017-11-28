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
package com.github.nfalco79.junit4osgi.runner.internal.jmx;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;

import com.j256.simplejmx.server.JmxServer;

public class JMXServer {

	private Logger logger;

	protected boolean jmxSupportEnabled;
	protected JmxServer jmxServer;

	public JMXServer() {
		try {
			Class.forName("com.j256.simplejmx.server.JmxServer");
			jmxSupportEnabled = true;

			jmxServer = new JmxServer(true);
		} catch (ClassNotFoundException e) {
			getLog().log(Level.INFO, "simplejmx is not installed, JUnit registry and runner will not be present in the MBeanService");
		}
	}

	public void start() {
		if (jmxSupportEnabled) {
			try {
				getLog().log(Level.FINE, "Starting JMX Server");
				jmxServer.start();
			} catch (JMException e) {
				getLog().log(Level.WARNING, "Cannot start the server", e);
			}
		}
	}

	public void stop() {
		if (jmxSupportEnabled && jmxServer != null) {
			getLog().log(Level.FINE, "Stopping JMX Server");
			jmxServer.stop();
		}
	}

	public void register(Object obj) {
		if (jmxSupportEnabled && jmxServer != null && obj != null) {
			try {
				jmxServer.register(obj);
				getLog().log(Level.FINE, "Register JMX object " + obj.getClass().getName());
			} catch (JMException e) {
				getLog().log(Level.WARNING, "Cannot register the " + obj.toString() + " as mbean", e);
			}
		}
	}

	public void unregister(Object obj) {
		if (jmxSupportEnabled && jmxServer != null && obj != null) {
			jmxServer.unregister(obj);
			getLog().log(Level.FINE, "Unregister JMX object " + obj.getClass().getName());
		}
	}

	private Logger getLog() {
		if (logger == null) {
			logger = Logger.getLogger(JMXServer.class.getName());
		}
		return logger;
	}
}