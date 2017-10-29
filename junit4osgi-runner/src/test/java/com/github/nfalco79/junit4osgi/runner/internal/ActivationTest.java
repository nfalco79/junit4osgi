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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;

import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.runner.internal.jmx.JMXServer;

public class ActivationTest {

	@Test
	public void runner_does_not_start_with_autostart_set_to_false() throws Exception {
		String prop = System.clearProperty(JUnitRunner.RUNNER_AUTOSTART);

		JUnitRunner runner = null;
		try {
			runner = spy(new JUnitRunner());
			doNothing().when(runner).start();

			runner.activate();
			verify(runner, never()).start();
		} finally {
			if (prop != null) {
				System.setProperty(JUnitRunner.RUNNER_AUTOSTART, prop);
			}
			if (runner != null) {
				runner.stop();
				runner.deactivate();
			}
		}
	}

	@Test
	public void runner_starts_if_autostart_set_to_true() throws Exception {
		String prop = System.setProperty(JUnitRunner.RUNNER_AUTOSTART, "true");

		JUnitRunner runner = null;
		try {
			runner = spy(new JUnitRunner());
			doNothing().when(runner).start();

			runner.activate();
			verify(runner).start();
		} finally {
			if (prop != null) {
				System.setProperty(JUnitRunner.RUNNER_AUTOSTART, prop);
			} else {
				System.clearProperty(JUnitRunner.RUNNER_AUTOSTART);
			}
			if (runner != null) {
				runner.stop();
				runner.deactivate();
			}
		}
	}

	@Test
	public void component_deactivate_perform_shutdown_of_jmx_server() {
		final JMXServer jmxServer = mock(JMXServer.class);
		doNothing().when(jmxServer).start();
		doNothing().when(jmxServer).register(any());

		JUnitRunner runner = new JUnitRunner() {
			@Override
			protected JMXServer newJMXServer() {
				return jmxServer;
			}
		};
		TestRegistry registry = mock(TestRegistry.class);
		runner.setRegistry(registry);

		runner.deactivate();
		verify(jmxServer).stop();
		verify(jmxServer).unregister(runner);
		verify(jmxServer).unregister(registry);
	}

	@Test
	public void component_activate_raise_new_jmx_server() {
		final JMXServer jmxServer = mock(JMXServer.class);

		JUnitRunner runner = new JUnitRunner() {
			@Override
			protected JMXServer newJMXServer() {
				return jmxServer;
			}
		};

		TestRegistry registry = mock(TestRegistry.class);
		runner.setRegistry(registry);

		runner.activate();
		verify(jmxServer).start();
		verify(jmxServer).register(runner);
		verify(jmxServer).register(registry);
	}

}