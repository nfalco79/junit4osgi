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
