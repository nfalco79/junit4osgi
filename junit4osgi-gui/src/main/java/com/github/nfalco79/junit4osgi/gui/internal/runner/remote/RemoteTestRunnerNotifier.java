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
package com.github.nfalco79.junit4osgi.gui.internal.runner.remote;

import java.io.PrintStream;
import java.io.Serializable;

import org.junit.internal.TextListener;
import org.junit.runner.notification.RunListener;

import com.github.nfalco79.junit4osgi.runner.spi.TestRunnerNotifier;

public class RemoteTestRunnerNotifier implements TestRunnerNotifier, Serializable {
	private static final long serialVersionUID = -4883968836927980341L;

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public RunListener getRunListener() {
		return new TextListener(new PrintStream(System.out));
	}

}
