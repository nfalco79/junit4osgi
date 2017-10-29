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

/**
 * This listener is used to be notified when new JUnit is registered or is gone
 * because the bundle that contributes it is to be uninstalling.
 *
 * @author nikolasfalco
 */
public interface TestRegistryChangeListener {

	/**
	 * Fires a {@link TestRegistry} changes.
	 *
	 * @param event
	 *            a {@link TestRegistryEvent}
	 */
	void registryChanged(TestRegistryEvent event);

}
