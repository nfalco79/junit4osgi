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
package com.github.nfalco79.junit4osgi.registry.internal.asm;

import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.log.LogService;

public class BundleTestClassVisitor extends ClassVisitor {

	private Set<String> cache;
	private boolean testClass;
	private Bundle bundle;
	private LogService log;

	public BundleTestClassVisitor(Bundle bundle) {
		super(Opcodes.ASM6);
		this.bundle = bundle;

		// cache is for bundle, if bundle is stopped and started, byte code could be changed
		cache = new HashSet<String>();
		cache.add("junit/framework/TestCase");
		cache.add("junit/framework/TestSuite");
		cache.add("junit/framework/Test");
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return new MethodVisitor(this.api) {
			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				if (visible && desc.contains("org/junit/Test")) {
					testClass = true;
				}
				return super.visitAnnotation(desc, visible);
			}
		};
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (cache.contains(superName)) {
			if (isAbstract(access)) {
				// add the hierarchy, but not mark it as test class because is abstract
				cache.add(name);
			} else {
				testClass = !isInterface(access);
			}
		} else if (superName != null && !superName.startsWith("java/") && !superName.startsWith("junit/")) {
			// look up the superclass in the same bundle of test class
			URL entry = bundle.getEntry("/" + superName + ".class");
			if (entry == null) {
				// look up the superclass in a wired bundle
				entry = findInWiredBundle(superName);
			}
			if (entry != null) {
				// analyse the superclass and add it to cache
				ASMUtils.analyseByteCode(entry, this);
				if (isConcreteClass(access)) {
					testClass = testClass || cache.contains(superName);
				}
			}
		}
	}

	private boolean isInterface(int access) {
		return (access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE;
	}

	private boolean isAbstract(int access) {
		return (access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT;
	}

	private boolean isConcreteClass(int access) {
		return !isInterface(access) && !isAbstract(access);
	}

	private URL findInWiredBundle(final String superName) {
		String superClassName = superName.replace('/', '.');
		String packageName = superClassName.substring(0, superClassName.lastIndexOf('.'));

		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		if (wiring == null && log != null) {
			log.log(LogService.LOG_INFO, "No wiring for the bundle " + bundle.getSymbolicName() + "["
					+ bundle.getBundleId() + "] state: " + bundle.getState() + " to look up " + superClassName);
		}
		if (wiring != null) {
			Iterator<BundleWire> requiredWires = wiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE).iterator();

			while (!testClass && requiredWires.hasNext()) {
				final BundleWire wire = requiredWires.next();

				final String filter = wire.getRequirement().getDirectives().get("filter");
				if (filter != null && filter.contains(packageName)) {
					final Bundle bundle = wire.getProviderWiring().getBundle();
					return bundle.getEntry("/" + superName + ".class");
				}
			}
		}
		return null;
	}

	public boolean isTestClass() {
		return testClass;
	}

	public void reset() {
		testClass = false;
	}

	public void setLog(LogService log) {
		this.log = log;
	}

}