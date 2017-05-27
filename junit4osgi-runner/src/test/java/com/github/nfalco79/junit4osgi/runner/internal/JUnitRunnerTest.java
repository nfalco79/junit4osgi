package com.github.nfalco79.junit4osgi.runner.internal;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.example.SimpleTestCase;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.internal.util.collections.Sets;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;

public class JUnitRunnerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@SuppressWarnings("unchecked")
	@Test
	public void test_runner_no_immediate_shutdown() throws Exception {
		LogService logService = mock(LogService.class);

		final Set<TestBean> registryTests = getMockTests();
		TestRegistry registry = mock(TestRegistry.class);
		when(registry.getTests()).thenReturn(registryTests);

		final CountDownLatch latch = new CountDownLatch(2);

		JUnitRunner runner = spy(new JUnitRunner());
		when(runner.getRepeatTime()).thenReturn(1l);
		when(runner.getTestRunnable(any(File.class), any(Queue.class))).thenAnswer(new Answer<Runnable>() {
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
	public void test_runner_single_test() throws Exception {
		LogService logService = mock(LogService.class);

		final TestBean testToRun = mock(TestBean.class);
		when(testToRun.getId()).thenReturn("id1");

		TestRegistry registry = mock(TestRegistry.class);
		when(registry.getTests(any(String[].class))).thenReturn(Sets.newSet(testToRun));

		final AtomicInteger counter = new AtomicInteger(0);

		JUnitRunner runner = spy(new JUnitRunner());
		when(runner.getRepeatTime()).thenReturn(1l);
		when(runner.getTestRunnable(any(File.class), any(Queue.class))).thenAnswer(new Answer<Runnable>() {
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
		runner.start(new String[] { testToRun.getId() }, null);
		Thread.sleep(runner.getRepeatTime() * 4);
		runner.stop();

		verify(registry).getTests(new String[] { testToRun.getId() });
		assertThat(counter.get(), Matchers.is(1));
		verify(registry, never()).addTestRegistryListener(any(TestRegistryChangeListener.class));
		verify(registry, never()).removeTestRegistryListener(any(TestRegistryChangeListener.class));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void test_runner_a_test() throws Exception {
		LogService logService = mock(LogService.class);

		final TestBean testToRun = mock(TestBean.class);
		when(testToRun.getId()).thenReturn("id1");
		when(testToRun.getTestClass()).thenReturn((Class) SimpleTestCase.class);

		TestRegistry registry = mock(TestRegistry.class);
		when(registry.getTests(any(String[].class))).thenReturn(Sets.newSet(testToRun));

		final CountDownLatch latch = new CountDownLatch(1);

		JUnitRunner runner = new JUnitRunner() {
			@Override
			protected Runnable getTestRunnable(File reportsDirectory, java.util.Queue<TestBean> tests) {
				final Runnable realRunnable = super.getTestRunnable(reportsDirectory, tests);
				return new Runnable() {
					@Override
					public void run() {
						realRunnable.run();
						latch.countDown();
					}
				};
			}
		};

		runner.setLog(logService);
		runner.setRegistry(registry);
		File tmpFolder = folder.newFolder();
		runner.start(new String[] { testToRun.getId() }, tmpFolder.toString());
		latch.await();
		runner.stop();

		File reportFile = new File(tmpFolder, "TEST-" + testToRun.getTestClass().getName() + ".xml");
		assertTrue(reportFile.isFile());
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
