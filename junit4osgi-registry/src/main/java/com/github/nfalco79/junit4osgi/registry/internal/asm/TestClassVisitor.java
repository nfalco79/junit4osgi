package com.github.nfalco79.junit4osgi.registry.internal.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TestClassVisitor extends ClassVisitor {

	private boolean testClass;

	public TestClassVisitor() {
		super(Opcodes.ASM6);
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
		if ("junit/framework/TestCase".equals(superName)) {
			testClass = true;
		}
	}

	public boolean isTestClass() {
		return testClass;
	}

	public void setTestClass(boolean testClass) {
		this.testClass = testClass;
	}

}