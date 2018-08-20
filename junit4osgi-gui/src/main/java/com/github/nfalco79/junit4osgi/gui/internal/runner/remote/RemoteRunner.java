package com.github.nfalco79.junit4osgi.gui.internal.runner.remote;

import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;

import com.github.nfalco79.junit4osgi.gui.internal.runner.TestExecutor;
import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.j256.simplejmx.client.JmxClient;

public class RemoteRunner implements TestExecutor {

	private final String connectorAddress;
	private MBeanInfo mBeanInfo;
	private JmxClient jmxClient;

	public RemoteRunner(final String jmxURL) {
		this.connectorAddress = jmxURL;

		try {
			jmxClient = new JmxClient(jmxURL);
		} catch (JMException e) {
			e.printStackTrace(System.err);
		}
	}

	@Override
	public String[] getTestsId() {
		try {
			ObjectName objectName = new ObjectName("org.osgi.junit4osgi:name=AutoDiscoveryRegistry,type=registry");
			Object attribute = jmxClient.getAttribute(objectName, "testIds");
			return (String[]) attribute;
		} catch (Exception e) {
		}
		return new String[0];
	}

	@Override
	public Set<TestBean> getTests(Set<String> testsId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeTestRegistryListener(TestRegistryChangeListener registryListener) {
		// TODO Auto-generated method stub

	}

}
