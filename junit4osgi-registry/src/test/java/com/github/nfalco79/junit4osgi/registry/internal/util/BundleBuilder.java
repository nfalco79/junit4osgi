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
package com.github.nfalco79.junit4osgi.registry.internal.util;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
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
	private Hashtable<String, String> manifestEntries;
	private Map<String, File> bundleResources;
	private URLStrategy strategy;
	private Map<String, BundleWire> wires;
	private int bundleState;

	private BundleBuilder() {
		bundle = mock(Bundle.class);
		bundleClasses = new ArrayList<Class<?>>();
		bundleResources = new HashMap<String, File>();
		strategy = new DefaultURLStrategy();
		wires = new HashMap<String, BundleWire>();
		bundleState = Bundle.RESOLVED;
		manifestEntries = new Hashtable<String, String>();
	}

	public static BundleBuilder newBuilder() {
		return new BundleBuilder();
	}

	public BundleBuilder symbolicName(String symbolicName) {
		Assert.assertNotNull(symbolicName);
		manifestEntries.put("BundleSymbolicName", symbolicName);

		when(bundle.getSymbolicName()).thenReturn(symbolicName);
		return this;
	}

	public BundleBuilder manifest(Map<String, String> entries) {
		Assert.assertNotNull(entries);

		manifestEntries.putAll(entries);
		return this;
	}

	public BundleBuilder manifestEntry(String header, String value) {
		Assert.assertNotNull(header);

		manifestEntries.put(header, value);
		return this;
	}

	public BundleBuilder state(int bundleState) {
		Assert.assertTrue(bundleState > 0);

		this.bundleState = bundleState;
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
		when(bundle.getState()).thenReturn(bundleState);
		when(bundle.getHeaders()).thenReturn(manifestEntries);

		for (Class<?> clazz : bundleClasses) {
			when(bundle.getEntry(toResource(clazz))).thenReturn(strategy.resolveURL(clazz));
			when(bundle.loadClass(clazz.getName())).thenReturn((Class) clazz);
		}
		if (bundleClasses.isEmpty()) {
			when(bundle.getEntry(anyString())).thenAnswer(new Answer<URL>() {
				@Override
				public URL answer(InvocationOnMock invocation) throws Throwable {
					String entry = (String) invocation.getArgument(0);
					return strategy.resolveURL(entry);
				}
			});
		} else {
			Map<String, SortedSet<String>> entries = asEntryPaths(bundleClasses);
			for (String path : entries.keySet()) {
				when(bundle.getEntryPaths(path)).thenReturn(new Vector<String>(entries.get(path)).elements());
			}
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

	private Map<String, SortedSet<String>> asEntryPaths(Collection<Class<?>> testsClass) throws MalformedURLException {
		Map<String, SortedSet<String>> paths = new HashMap<String, SortedSet<String>>();
		for (Class<?> bundleClass : bundleClasses) {
			String classPath = bundleClass.getName().replace('.', '/') + ".class";
			StringTokenizer st = new StringTokenizer(classPath, "/");
			String root = null;
			if (st.hasMoreTokens()) {
				root = st.nextToken();
				if (!root.endsWith(".class")) {
					root += "/";
				}
			}
			while (st.hasMoreTokens()) {
				String nextToken = st.nextToken();

				SortedSet<String> segment = paths.get(root);
				if (segment == null) {
					paths.put(root, segment = new TreeSet<String>());
				}

				root = root + nextToken;
				if (!root.endsWith(".class")) {
					root += "/";
				}

				segment.add(root);
			}
		}
		paths.put("/", new TreeSet<String>(paths.keySet()));
		return paths;
	}

}