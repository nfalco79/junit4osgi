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
package com.github.nfalco79.junit4osgi.registry.spi;

import java.util.Set;

import org.osgi.framework.Bundle;

/**
 * This is an OSGi test registry that keeps all JUnit3/4 classes contained in an
 * OSGi bundle that is in state resolved in the platform.
 *
 * @author nikolasfalco
 */
public interface TestRegistry {

	/**
	 * Register all JUnit classes contained in the given contributor bundle.
	 *
	 * @param contributor
	 *            the bundle into lookup
	 */
	void registerTests(Bundle contributor);

	/**
	 * Unregister all JUnit classes contained in the given contributor bundle.
	 * <p>
	 * If the contributor did never registered than nothing will happens.
	 *
	 * @param contributor
	 *            a registered the bundle registered
	 */
	void removeTests(Bundle contributor);

	/**
	 * Returns a set of {@link TestBean} to provides all the JUnit class in the
	 * registry.
	 *
	 * @return a set of all registered {@link TestBean}
	 */
	Set<TestBean> getTests();

	/**
	 * Returns a set of {@link TestBean} that matches the given test id.
	 *
	 * @param testIds
	 *            the requested {@link TestBean}
	 * @return an array of {@link TestBean} that match the given identifiers.
	 */
	Set<TestBean> getTests(String[] testIds);

	/**
	 * Register a {@link TestRegistryChangeListener} used to be notified each
	 * time a new JUnit test is registered or is gone.
	 *
	 * @param listener
	 *            a {@link TestRegistryChangeListener} implementation
	 */
	void addTestRegistryListener(TestRegistryChangeListener listener);

	/**
	 * Removes a {@link TestRegistryChangeListener} registered previously.
	 * <p>
	 * If the listener instance was not registered nothing happens.
	 *
	 * @param listener
	 *            the {@link TestRegistryChangeListener} implementation to
	 *            remove
	 */
	void removeTestRegistryListener(TestRegistryChangeListener listener);

}