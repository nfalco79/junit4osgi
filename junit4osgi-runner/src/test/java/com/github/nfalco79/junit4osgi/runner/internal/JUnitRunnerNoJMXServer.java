package com.github.nfalco79.junit4osgi.runner.internal;

import com.github.nfalco79.junit4osgi.runner.internal.jmx.JMXServer;

/*pacakge*/ class JUnitRunnerNoJMXServer extends JUnitRunner {

	@Override
	protected JMXServer newJMXServer() {
		return new JMXServerMock();
	}

}