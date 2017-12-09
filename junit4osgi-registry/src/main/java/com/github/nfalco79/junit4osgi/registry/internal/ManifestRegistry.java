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
package com.github.nfalco79.junit4osgi.registry.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.internal.asm.BundleTestClassVisitor;
import com.github.nfalco79.junit4osgi.registry.spi.AbstractTestRegistry;
import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent.TestRegistryEventType;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;

@JmxResource(domainName = "org.osgi.junit4osgi", folderNames = "type=registry", beanName = "ManifestRegistry", description = "The JUnit4 registry that collect tests from the MANIFEST header Test-Suite")
public final class ManifestRegistry extends AbstractTestRegistry {
	public static final String TEST_ENTRY = "Test-Suite";

	@JmxOperation(description = "Dispose the registry")
	@Override
	public void dispose() {
		super.dispose();
	}

	@JmxAttributeMethod(description = "Returns a list of all tests id in the registry")
	public String[] getTestIds() {
		Set<String> allTests = new LinkedHashSet<String>();
		for (Set<TestBean> bundleTests : tests.values()) {
			for (TestBean test : bundleTests) {
				allTests.add(test.getId());
			}
		}
		return allTests.toArray(new String[allTests.size()]);
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.nfalco79.junit4osgi.registry.spi.TestRegistry#registerTests(org.osgi.framework.Bundle)
	 */
	@Override
	public void registerTests(Bundle contributor) {
		if (tests.containsKey(contributor)) {
			return;
		}
		parseManifest(contributor);
	}

	private void parseManifest(Bundle bundle) {
		Set<TestBean> bundleTest = new LinkedHashSet<TestBean>();
		tests.put(bundle, bundleTest);

		final String symbolicName = bundle.getSymbolicName();

		final URL resource = bundle.getEntry("META-INF/MANIFEST.MF");
		if (resource == null) {
			getLog().log(LogService.LOG_WARNING,
					"No MANIFEST for bundle " + symbolicName + "[id:" + bundle.getBundleId() + "]");
			return;
		}

		InputStream is = null;
		try {
			is = resource.openStream();
			Manifest mf = new Manifest(is);

			// should fragments be manage differently??
			final String value = mf.getMainAttributes().getValue(TEST_ENTRY);
			if (value != null && !"".equals(value)) {
				BundleTestClassVisitor visitor = new BundleTestClassVisitor(bundle);
				visitor.setLog(getLog());

				StringTokenizer st = new StringTokenizer(value, ",");
				while (st.hasMoreTokens()) {
					String className = st.nextToken().trim();

					TestBean bean = null;
					try {
						bean = new TestBean(bundle, className);
					} catch (IllegalArgumentException e) {
						getLog().log(LogService.LOG_ERROR,
								"Test class '" + className + "' could not be found in the bundle " + symbolicName, e);
						continue;
					}

					if (isTestClass(bundle, bean, visitor)) {
						bundleTest.add(bean);

						fireEvent(new TestRegistryEvent(TestRegistryEventType.ADD, bean));
					}
				}
			}
		} catch (IOException e) {
			getLog().log(LogService.LOG_ERROR, "Could not read MANIFEST of bundle " + symbolicName, e);
		} finally {
			closeSilently(is);
		}
	}

	private void closeSilently(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				// close stream silently
			}
		}
	}

}