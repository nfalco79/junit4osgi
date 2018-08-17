package com.github.nfalco79.junit4osgi.gui.runner.remote;

public class VirtualMachineDetails {

	private final String description;
	private final String jmxURL;

	public VirtualMachineDetails(final String jmxURL, final String description) {
		this.jmxURL = jmxURL;
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public String getJmxURL() {
		return jmxURL;
	}
}
