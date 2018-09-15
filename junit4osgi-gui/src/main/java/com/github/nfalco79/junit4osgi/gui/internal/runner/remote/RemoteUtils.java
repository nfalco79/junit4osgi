package com.github.nfalco79.junit4osgi.gui.internal.runner.remote;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.j256.simplejmx.client.JmxClient;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

@SuppressWarnings("restriction")
public final class RemoteUtils {
	private static final String JAVA_RUNTIME_VERSION = "java.runtime.version";
	private static final String JMX_LOCAL_CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

	private static boolean toolsAvailable = isToolsAvailable();
	private static int javaVersion = getJavaVersion();
	private static Method startLocalJMX = getStartLocalJMXMethod();

	public static boolean isAutodetectEnabled() {
		return toolsAvailable;
	}

	public static List<VirtualMachineDetails> listVMs() {
		if (!toolsAvailable) {
			return Collections.emptyList();
		}

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

	private static boolean isToolsAvailable() {
		try {
			Class.forName("com.sun.tools.attach.VirtualMachine");
			return true;
		} catch (ClassNotFoundException e) {
			// tools.jar not in classpath
		} catch (NoClassDefFoundError e) {
			// tools.jar in classpath but some issue on load class (are in OSGi)
		}
		return false;
	}

	public static boolean hasJUnit4OSGi(String connectorAddress) {
		JmxClient client = null;
		try {
			client = new JmxClient(connectorAddress);
			return !client.getBeanNames("org.osgi.junit4osgi").isEmpty();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			if (client != null) {
				client.close();
			}
		}
		return false;
	}

	private static Method getStartLocalJMXMethod() {
		if (toolsAvailable && javaVersion >= 8) {
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

	private RemoteUtils() {
	}
}