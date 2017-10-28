package com.github.nfalco79.junit4osgi.registry.internal.asm;

import static org.junit.Assert.assertThat;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.example.hierarchy.AbstractHierarchyTestCase;
import org.example.hierarchy.HierarchyBaseTestCase;
import org.example.hierarchy.HierarchyTestCase;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.Bundle;

import com.github.nfalco79.junit4osgi.registry.internal.asm.ASMUtils;
import com.github.nfalco79.junit4osgi.registry.internal.asm.BundleTestClassVisitor;
import com.github.nfalco79.junit4osgi.registry.internal.util.BundleBuilder;

import junit.framework.TestCase;

@RunWith(Parameterized.class)
public class BundleClassVisitorTest {
	@Parameters(name = "{0} {2}")
	public static Collection<Object[]> data() {
		return Arrays.asList(
				new Object[][] { { HierarchyTestCase.class, true, "hierarchy extends TestCase it's a JUnit3" },
						{ HierarchyBaseTestCase.class, true, "hierarchy extends TestCase it's a JUnit3" },
						{ AbstractHierarchyTestCase.class, false,
								"it's an abstract class that extends TestCase, it's not a JUnit3" } });
	}

	private Class<?> testClass;
	private boolean isTestCase;
	private String reason;

	public BundleClassVisitorTest(Class<?> testClass, boolean isTestCase, String assertMessage) {
		this.testClass = testClass;
		this.isTestCase = isTestCase;
		this.reason = assertMessage;
	}

	@Test
	public void asm_visit_hierarchy_of_junit3_class() throws Exception {
		Bundle bundle = getMockBundle(testClass);
		URL resource = getClass().getResource(BundleBuilder.toResource(testClass));

		BundleTestClassVisitor visitor = new BundleTestClassVisitor(bundle);
		ASMUtils.analyseByteCode(resource, visitor);

		assertThat(reason, visitor.isTestClass(), Matchers.is(isTestCase));
	}

	private Bundle getMockBundle(Class<?>... classes) throws Exception {
		Set<Class<?>> superClasses = new LinkedHashSet<Class<?>>();
		for (Class<?> clazz : classes) {
			Class<?> superclass = clazz.getSuperclass();
			while (superclass != Object.class && superclass != TestCase.class) {
				superClasses.add(superclass);
				superclass = superclass.getSuperclass();
			}
		}
		Bundle wiredBundle = BundleBuilder.newBuilder() //
				.symbolicName("acme.wired") //
				.addClasses(superClasses.toArray(new Class<?>[0])) //
				.build();

		BundleBuilder builder = BundleBuilder.newBuilder() //
				.symbolicName("acme") //
				.addClasses(classes);

		for (Class<?> clazz : classes) {
			builder.wire(wiredBundle, clazz.getPackage().getName());
		}

		return builder.build();
	}

}