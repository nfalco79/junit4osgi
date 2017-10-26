package com.github.nfalco79.junit4osgi.registry.util;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public final class BundleBuilder {

	public interface URLStrategy {
		URL resolveURL(Class<?> resource) throws MalformedURLException;

		URL resolveURL(String entry);
	}

	private class DefaultURLStrategy implements URLStrategy {
		@Override
		public URL resolveURL(Class<?> resource) {
			return resource.getResource(toResource(resource));
		}

		@Override
		public URL resolveURL(String entry) {
			try {
				return new URL(entry);
			} catch (MalformedURLException e) {
				return null;
			}
		}
	}

	private Bundle bundle;
	private List<Class<?>> bundleClasses;
	private Map<String, File> bundleResources;
	private URLStrategy strategy;
	private Map<String, BundleWire> wires;

	private BundleBuilder() {
		bundle = mock(Bundle.class);
		bundleClasses = new ArrayList<Class<?>>();
		bundleResources = new HashMap<String, File>();
		strategy = new DefaultURLStrategy();
		wires = new HashMap<String, BundleWire>();
	}

	public static BundleBuilder newBuilder() {
		return new BundleBuilder();
	}

	public BundleBuilder symbolicName(String symbolicName) {
		Assert.assertNotNull(symbolicName);

		when(bundle.getSymbolicName()).thenReturn(symbolicName);
		return this;
	}

	public BundleBuilder addClass(Class<?> bundleClass) {
		Assert.assertNotNull(bundleClass);

		bundleClasses.add(bundleClass);
		return this;
	}

	public BundleBuilder addResource(String path, File resource) {
		Assert.assertNotNull(path);
		Assert.assertNotNull(resource);

		bundleResources.put(path, resource);
		return this;
	}

	public BundleBuilder addClasses(Class<?>... bundleClasses) {
		Assert.assertNotNull(bundleClasses);

		for (Class<?> bundleClass : bundleClasses) {
			addClass(bundleClass);
		}
		return this;
	}

	public BundleBuilder urlStrategy(URLStrategy strategy) {
		Assert.assertNotNull(strategy);

		this.strategy = strategy;
		return this;
	}

	public void wire(Bundle bundle, String... packagesName) {
		Assert.assertNotNull(bundle);
		Assert.assertNotNull(packagesName);

		BundleWire wire = mock(BundleWire.class);

		BundleRequirement bundleRequirement = mock(BundleRequirement.class);
		when(wire.getRequirement()).thenReturn(bundleRequirement);

		Map<String, String> directives = new HashMap<String, String>(packagesName.length);
		for (String packageName : packagesName) {
			Assert.assertThat(wires, Matchers.not(Matchers.hasEntry(Matchers.equalTo(packageName), Matchers.any(BundleWire.class))));

			wires.put(packageName, wire);
			directives.put("filter", "(&(" + BundleRevision.PACKAGE_NAMESPACE + '=' + packageName + "))");
		}
		when(bundleRequirement.getDirectives()).thenReturn(directives);

		BundleWiring bundleWiring = mock(BundleWiring.class);
		when(bundleWiring.getBundle()).thenReturn(bundle);

		when(wire.getProviderWiring()).thenReturn(bundleWiring);
	}

	public Bundle build() throws Exception {
		when(bundle.findEntries("/", "*.class", true)).thenReturn(toURLs(bundleClasses));

		for (Class<?> clazz : bundleClasses) {
			when(bundle.getEntry(toResource(clazz))).thenReturn(strategy.resolveURL(clazz));
		}
		if (bundleClasses.isEmpty()) {
			when(bundle.getEntry(anyString())).thenAnswer(new Answer<URL>() {
				@Override
				public URL answer(InvocationOnMock invocation) throws Throwable {
					String entry = (String) invocation.getArgument(0);
					return strategy.resolveURL(entry);
				}
			});
		}

		for (Entry<String, File> resEntry : bundleResources.entrySet()) {
			when(bundle.getEntry(resEntry.getKey())).thenReturn(resEntry.getValue().toURI().toURL());
		}

		BundleWiring bundleWiring = mock(BundleWiring.class);
		when(bundleWiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE)).thenReturn(new ArrayList<BundleWire>(wires.values()));

		when(bundle.adapt(BundleWiring.class)).thenReturn(bundleWiring);

		return bundle;
	}

	public static String toResource(Class<?> clazz) {
		return '/' + clazz.getName().replace('.', '/') + ".class";
	}

	private Enumeration<URL> toURLs(Collection<Class<?>> testsClass) throws MalformedURLException {
		Vector<URL> resources = new Vector<URL>(bundleClasses.size());
		for (Class<?> testClass : bundleClasses) {
			resources.add(strategy.resolveURL(testClass));
		}
		return resources.elements();
	}

}