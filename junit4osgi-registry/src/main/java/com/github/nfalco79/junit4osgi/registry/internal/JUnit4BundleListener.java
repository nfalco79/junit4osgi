/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;

public class JUnit4BundleListener implements BundleListener {
	private TestRegistry registry;

	public JUnit4BundleListener(TestRegistry registry) {
		this.registry = registry;
	}

	public void addBundle(Bundle bundle) {
		switch (bundle.getState())  {
		case Bundle.INSTALLED:
			registerTestCase(bundle);
			break;
		case Bundle.STOPPING:
			unregisterTestCase(bundle);
			break;
		default:
			break;
		}
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		Bundle bundle = event.getBundle();
		switch (event.getType()) {
			case BundleEvent.INSTALLED:
			case BundleEvent.STARTED:
				registerTestCase(bundle);
				break;
			case BundleEvent.STOPPED:
			case BundleEvent.UNINSTALLED:
				unregisterTestCase(bundle);
				break;
			default:
				break;
		}
	}

	private void unregisterTestCase(Bundle bundle) {
		getRegistry().removeTests(bundle);
	}

	private void registerTestCase(Bundle bundle) {
		getRegistry().registerTests(bundle);
	}

	/* package */TestRegistry getRegistry() {
		return registry;
	}

}