package com.github.nfalco79.junit4osgi.gui.runner.remote;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

@SuppressWarnings("restriction")
public class JVMUtils {
	private static final String JAVA_RUNTIME_VERSION = "java.runtime.version";
	private static final String JMX_LOCAL_CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

	private static int javaVersion = getJavaVersion();
	private static Method startLocalJMX = getStartLocalJMXMethod();

	public static List<VirtualMachineDetails> listVMs() {
		List<VirtualMachineDetails> vms = new ArrayList<VirtualMachineDetails>();

		for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
			VirtualMachine vm = null;
			try {
				vm = VirtualMachine.attach(descriptor);
				String version = vm.getSystemProperties().getProperty(JAVA_RUNTIME_VERSION);
				String jvmDescription = descriptor.displayName() + " (runtime " + version + ")";

				String connectorAddress = vm.getAgentProperties().getProperty(JMX_LOCAL_CONNECTOR_ADDRESS);
				if (connectorAddress == null && startLocalJMX != null) {
					connectorAddress = (String) startLocalJMX.invoke(vm);
				}
				if (connectorAddress == null) {
					continue;
				}
				if (hasJUnit4OSGi(connectorAddress)) {
					vms.add(new VirtualMachineDetails(connectorAddress, jvmDescription));
				}
			} catch (Exception e) {
				e.printStackTrace(System.err);
			} finally {
				try {
					if (vm != null) {
						vm.detach();
					}
				} catch (IOException e) {
				}
			}
		}

		return vms;
	}

	public static boolean hasJUnit4OSGi(String connectorAddress) {
		try {
			JMXServiceURL url = new JMXServiceURL(connectorAddress);
			JMXConnector connector = JMXConnectorFactory.connect(url);
			MBeanServerConnection mbs = connector.getMBeanServerConnection();

			ObjectName registry = new ObjectName("org.osgi.junit4osgi:name=AutoDiscoveryRegistry,type=registry");
			return mbs.getMBeanInfo(registry) != null;
		} catch (JMException e) {
			return false;
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		return false;
	}

	private static Method getStartLocalJMXMethod() {
		if (javaVersion >= 8) {
			try {
				return VirtualMachine.class.getDeclaredMethod("startLocalManagementAgent");
			} catch (NoSuchMethodException e) {
			} catch (SecurityException e) {
			}
		}
		return null;
	}

	private static int getJavaVersion() {
		String javaRuntimeVersion = System.getProperty(JAVA_RUNTIME_VERSION);
		int javaVersion = 6;

		if (javaRuntimeVersion != null) {
			String[] versionParts = javaRuntimeVersion.split("\\.");
			if (versionParts.length > 2) {
				try {
					javaVersion = Integer.parseInt(versionParts[1]);
				} catch (NumberFormatException e) {
				}
			}
		}

		return javaVersion;
	}

	public static void main(String[] args) {
		listVMs();
	}
}
