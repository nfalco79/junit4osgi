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
package com.github.nfalco79.junit4osgi.runner.internal;

import static java.util.Arrays.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.example.AbstractTest;
import org.example.ErrorTest;
import org.example.FlakyJUnit4Test;
import org.example.JUnit3Test;
import org.example.MainClassTest;
import org.example.SimpleSuiteTest;
import org.example.SimpleTestCase;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.mockito.internal.util.collections.Sets;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.github.nfalco79.junit4osgi.runner.spi.TestRunnerNotifier;

public class JUnitRunnerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@SuppressWarnings("unchecked")
	@Test
	public void verify_runner_schedule_job_when_no_immediate_shutdown_is_set() throws Exception {
		LogService logService = mock(LogService.class);

		final Set<TestBean> registryTests = getMockTests();
		TestRegistry registry = mock(TestRegistry.class);
		when(registry.getTests()).thenReturn(registryTests);

		final CountDownLatch latch = new CountDownLatch(2);

		JUnitRunner runner = spy(new JUnitRunnerNoJMXServer());
		when(runner.getRepeatTime()).thenReturn(1l);
		when(runner.getInfiniteRunnable(any(File.class), any(Queue.class))).thenAnswer(new Answer<Runnable>() {
			@Override
			public Runnable answer(InvocationOnMock invocation) throws Throwable {
				Queue<TestBean> tests = (Queue<TestBean>) invocation.getArgument(1);
				assertArrayEquals(registryTests.toArray(), tests.toArray());
				return new Runnable() {
					@Override
					public void run() {
						latch.countDown();
					}
				};
			}
		});

		runner.setLog(logService);
		runner.setRegistry(registry);
		runner.start();
		assertThat(runner.isStopped(), CoreMatchers.is(false));
		try {
			latch.await(runner.getRepeatTime() * 4, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			fail("The runnable seems not be scheduled continuosly");
		}
		runner.stop();

		verify(registry, atLeastOnce()).getTests();
		verify(registry).addTestRegistryListener(any(TestRegistryChangeListener.class));
		verify(registry).removeTestRegistryListener(any(TestRegistryChangeListener.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void run_a_test() throws Exception {
		LogService logService = mock(LogService.class);

		final TestBean testToRun = mock(TestBean.class);
		when(testToRun.getId()).thenReturn("id1");

		TestRegistry registry = mock(TestRegistry.class);
		when(registry.getTests(any(String[].class))).thenReturn(Sets.newSet(testToRun));

		final AtomicInteger counter = new AtomicInteger(0);

		JUnitRunner runner = spy(new JUnitRunnerNoJMXServer());
		when(runner.getSingleRunnable(any(File.class), any(Queue.class), any(TestRunnerNotifier.class))).thenAnswer(new Answer<Runnable>() {
			@Override
			public Runnable answer(InvocationOnMock invocation) throws Throwable {
				Queue<TestBean> tests = (Queue<TestBean>) invocation.getArgument(1);
				assertThat(tests.size(), CoreMatchers.is(1));
				assertThat(tests, CoreMatchers.hasItem(testToRun));
				return new Runnable() {
					@Override
					public void run() {
						counter.incrementAndGet();
					}
				};
			}
		});

		runner.setLog(logService);
		runner.setRegistry(registry);
		runner.start(new String[] { testToRun.getId() }, null, new SafeTestRunnerNotifier(null, logService));
		Thread.sleep(100l);
		runner.stop();

		verify(registry).getTests(new String[] { testToRun.getId() });
		assertThat(counter.get(), Matchers.is(1));
		verify(registry, never()).addTestRegistryListener(any(TestRegistryChangeListener.class));
		verify(registry, never()).removeTestRegistryListener(any(TestRegistryChangeListener.class));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void run_a_real_test_case() throws Exception {
		final TestBean testToRun = mock(TestBean.class);
		when(testToRun.getId()).thenReturn("id1");
		when(testToRun.getTestClass()).thenReturn((Class) SimpleTestCase.class);

		File tmpFolder = folder.newFolder();

		runTest(tmpFolder, testToRun);

		File reportFile = new File(tmpFolder, "TEST-" + testToRun.getTestClass().getName() + ".xml");
		assertTrue("Test has not run", reportFile.isFile());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void run_a_flaky_tests() throws Exception {
		FlakyJUnit4Test.reset();

		final TestBean testToRun = mock(TestBean.class);
		when(testToRun.getId()).thenReturn("id1");
		when(testToRun.getTestClass()).thenReturn((Class) FlakyJUnit4Test.class);

		File tmpFolder = folder.newFolder();

		JUnitRunner runner = new StartAndStopJUnitRunner();
		runner.setRerunFailingTests(2);
		final RunListener listener = runTest(runner, tmpFolder, testToRun);

		verify(listener, times(6)).testStarted(any(Description.class));
		verify(listener, times(5)).testFailure(any(Failure.class));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void verify_that_runner_does_not_start_without_a_log_service() throws Exception {
		final TestBean testToRun = mock(TestBean.class);
		when(testToRun.getId()).thenReturn("id1");
		when(testToRun.getTestClass()).thenReturn((Class) SimpleTestCase.class);

		TestRegistry registry = mock(TestRegistry.class);
		when(registry.getTests(any(String[].class))).thenReturn(Sets.newSet(testToRun));

		JUnitRunner runner = spy(new JUnitRunnerNoJMXServer());
		runner.setLog(null);
		runner.setRegistry(registry);
		runner.start();
		verify(runner, never()).isRunning();
		verify(runner, never()).getInfiniteRunnable(any(File.class), any(Queue.class));
		runner.stop();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void verify_that_runner_does_not_start_invalid_test_cases() throws Exception {
		final TestBean test1ToRun = mock(TestBean.class);
		when(test1ToRun.getId()).thenReturn("id1");
		when(test1ToRun.getTestClass()).thenReturn((Class) AbstractTest.class);

		final TestBean test2ToRun = mock(TestBean.class);
		when(test2ToRun.getId()).thenReturn("id2");
		when(test2ToRun.getTestClass()).thenReturn((Class) SimpleTestCase.class);

		final TestBean test3ToRun = mock(TestBean.class);
		when(test3ToRun.getId()).thenReturn("id3");
		when(test3ToRun.getTestClass()).thenReturn((Class) MainClassTest.class);

		JUnitRunner runner = new StartAndStopJUnitRunner();
		runner.setExcludes(asSet("*Simple*"));

		File tmpFolder = folder.newFolder();
		runTest(runner, tmpFolder, test1ToRun, test2ToRun, test3ToRun);

		assertThat("Tests has run", tmpFolder.list(), Matchers.arrayWithSize(0));
	}

	private <T> Set<T> asSet(final T... testsToRun) {
		return new LinkedHashSet<T>(asList(testsToRun));
	}

	private void runTest(File destination, TestBean... testsToRun) throws Exception {
		runTest(null, destination, testsToRun);
	}

	/*
	 * Runs real test using a configured runner.
	 */
	private RunListener runTest(JUnitRunner runner, File destination, TestBean... testsToRun) throws Exception {
		LogService logService = mock(LogService.class);

		TestRegistry registry = mock(TestRegistry.class);
		when(registry.getTests(any(String[].class))).thenReturn(asSet(testsToRun));

		if (runner == null) {
			runner = new StartAndStopJUnitRunner();
		}

		runner.setLog(logService);
		runner.setRegistry(registry);

		String[] ids = new String[testsToRun.length];
		for (int i = 0; i < testsToRun.length; i++) {
			ids[i] = testsToRun[i].getId();
		}

		TestRunnerNotifier notifier = spy(new SafeTestRunnerNotifier(null, logService));
		final RunListener listener = mock(RunListener.class);
		when(notifier.getRunListener()).thenReturn(listener);

		runner.start(ids, destination.toString(), notifier);
		runner.stop();

		return listener;
	}

	@Test
	public void test_includes() {
		Set<String> includes = new LinkedHashSet<String>();
		includes.add("*Test");

		JUnitRunner runner = new JUnitRunner();
		runner.setIncludes(includes);
		assertTrue(runner.accept(JUnit3Test.class));
	}

	@Test
	public void test_excludes() {
		Set<String> excludes = new LinkedHashSet<String>();
		excludes.add("*.?Unit*");

		JUnitRunner runner = new JUnitRunner();
		runner.setLog(mock(LogService.class));
		runner.setExcludes(excludes);
		assertFalse(runner.accept(JUnit3Test.class));
	}

	@Test
	public void test_includes_and_excludes() {
		Set<String> includes = new LinkedHashSet<String>();
		includes.add("*Test");
		Set<String> excludes = new LinkedHashSet<String>();
		excludes.add("*3*");
		excludes.add("*Simple*");

		JUnitRunner runner = new JUnitRunner();
		runner.setLog(mock(LogService.class));
		runner.setIncludes(includes);
		runner.setExcludes(excludes);
		assertTrue(runner.accept(ErrorTest.class));
		assertFalse(runner.accept(JUnit3Test.class));
		assertFalse(runner.accept(SimpleSuiteTest.class));
	}

	@Test
	public void by_default_includes_all() {
		JUnitRunner runner = new JUnitRunner();
		assertTrue(runner.accept(ErrorTest.class));
		assertTrue(runner.accept(JUnit3Test.class));
		assertTrue(runner.accept(SimpleSuiteTest.class));
	}

	private Set<TestBean> getMockTests() {
		TestBean test1 = mock(TestBean.class);
		when(test1.getId()).thenReturn("id1");

		TestBean test2 = mock(TestBean.class);
		when(test2.getId()).thenReturn("id2");

		Set<TestBean> tests = new HashSet<TestBean>();
		tests.add(test1);
		tests.add(test2);
		return tests;
	}

}