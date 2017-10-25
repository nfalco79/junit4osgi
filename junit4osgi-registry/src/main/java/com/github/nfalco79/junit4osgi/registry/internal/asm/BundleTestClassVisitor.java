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

public class BundleTestClassVisitor extends ClassVisitor {

	private Set<String> cache;
	private boolean testClass;
	private Bundle contributor;

	public BundleTestClassVisitor(Bundle bundle) {
		super(Opcodes.ASM6);
		this.contributor = bundle;

		// cache is for bundle, if bundle is stopped and started, byte code could be changed
		cache = new HashSet<String>();
		cache.add("junit/framework/TestCase");
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
				cache.add(name);
			} else {
				testClass = !isInterface(access);
			}
		} else if (superName != null && !superName.startsWith("java/") && !superName.startsWith("junit/")) {
			URL entry = contributor.getEntry("/" + superName + ".class");
			if (entry == null) {
				entry = findInWiredBundle(superName);
			}
			if (entry != null) {
				ASMUtils.analyseByteCode(entry, this);
				testClass = testClass || cache.contains(superName);
			}
		}
	}

	private boolean isInterface(int access) {
		return (access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE;
	}

	private boolean isAbstract(int access) {
		return (access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT;
	}

	private URL findInWiredBundle(final String superName) {
		String superClassName = superName.replace('/', '.');
		String packageName = superClassName.substring(0, superClassName.lastIndexOf('.'));
		BundleWiring wiring = contributor.adapt(BundleWiring.class);
		Iterator<BundleWire> requiredWires = wiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE).iterator();
		while (!testClass && requiredWires.hasNext()) {
			final BundleWire wire = requiredWires.next();
			final String filter = wire.getRequirement().getDirectives().get("filter");
			if (filter.contains(packageName)) {
				final Bundle bundle = wire.getProviderWiring().getBundle();
				return bundle.getEntry("/" + superName + ".class");
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

}