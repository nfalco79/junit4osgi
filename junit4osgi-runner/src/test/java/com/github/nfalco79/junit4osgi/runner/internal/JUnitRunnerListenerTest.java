package com.github.nfalco79.junit4osgi.runner.internal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;

import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent.TestRegistryEventType;

public class JUnitRunnerListenerTest {

	@SuppressWarnings("unchecked")
	@Test
	public void fire_event_on_new_test() throws Exception {
		LogService logService = mock(LogService.class);

		TestBean[] registryTests = getMockTests();
		final List<TestRegistryChangeListener> runnerListener = new ArrayList<TestRegistryChangeListener>(1);

		TestRegistry registry = mock(TestRegistry.class);
		when(registry.getTests()).thenReturn(Collections.<TestBean>emptySet());
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				runnerListener.add((TestRegistryChangeListener) invocation.getArgument(0));
				return null;
			}
		}).when(registry).addTestRegistryListener(any(TestRegistryChangeListener.class));

		JUnitRunner runner = spy(new JUnitRunner());
		when(runner.getRepeatTime()).thenReturn(1l);
		when(runner.getInfiniteRunnable(any(File.class), any(Queue.class))).thenReturn(mock(Runnable.class));

		runner.setLog(logService);
		runner.setRegistry(registry);
		runner.start();

		TestRegistryChangeListener listener = runnerListener.get(0);

		Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses("tests", listener.getClass());
		field.setAccessible(true);
		Queue<TestBean> testQueue = (Queue<TestBean>) spy(field.get(listener));
		field.set(listener, testQueue);

		for (TestBean test : registryTests) {
			listener.registryChanged(new TestRegistryEvent(TestRegistryEventType.ADD, test));
		}

		runner.stop();

		verify(testQueue).add(registryTests[0]);
		verify(testQueue).add(registryTests[1]);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void fire_event_when_remove_test() throws Exception {
		LogService logService = mock(LogService.class);

		TestBean[] registryTests = getMockTests();
		final List<TestRegistryChangeListener> runnerListener = new ArrayList<TestRegistryChangeListener>(1);

		TestRegistry registry = mock(TestRegistry.class);
		when(registry.getTests()).thenReturn(new HashSet<TestBean>(Arrays.asList(registryTests)));
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				runnerListener.add((TestRegistryChangeListener) invocation.getArgument(0));
				return null;
			}
		}).when(registry).addTestRegistryListener(any(TestRegistryChangeListener.class));

		JUnitRunner runner = spy(new JUnitRunner());
		when(runner.getRepeatTime()).thenReturn(1l);
		when(runner.getInfiniteRunnable(any(File.class), any(Queue.class))).thenReturn(mock(Runnable.class));

		runner.setLog(logService);
		runner.setRegistry(registry);
		runner.start();

		TestRegistryChangeListener listener = runnerListener.get(0);

		Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses("tests", listener.getClass());
		field.setAccessible(true);
		Queue<TestBean> testQueue = (Queue<TestBean>) spy(field.get(listener));
		field.set(listener, testQueue);

		for (TestBean test : registryTests) {
			listener.registryChanged(new TestRegistryEvent(TestRegistryEventType.REMOVE, test));
		}

		runner.stop();

		verify(testQueue).remove(registryTests[0]);
		verify(testQueue).remove(registryTests[1]);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = IllegalArgumentException.class)
	public void test_event_with_invalid_argument() throws Exception {
		LogService logService = mock(LogService.class);

		final List<TestRegistryChangeListener> runnerListener = new ArrayList<TestRegistryChangeListener>(1);

		TestRegistry registry = mock(TestRegistry.class);
		when(registry.getTests()).thenReturn(Collections.<TestBean>emptySet());
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				runnerListener.add((TestRegistryChangeListener) invocation.getArgument(0));
				return null;
			}
		}).when(registry).addTestRegistryListener(any(TestRegistryChangeListener.class));

		JUnitRunner runner = spy(new JUnitRunner());
		when(runner.getRepeatTime()).thenReturn(1l);
		when(runner.getInfiniteRunnable(any(File.class), any(Queue.class))).thenReturn(mock(Runnable.class));

		runner.setLog(logService);
		runner.setRegistry(registry);
		runner.start();
		runner.stop();

		TestRegistryChangeListener listener = runnerListener.get(0);
		listener.registryChanged(new TestRegistryEvent(TestRegistryEventType.ADD, null));
	}

	private TestBean[] getMockTests() {
		TestBean test1 = mock(TestBean.class);
		when(test1.getId()).thenReturn("id1");

		TestBean test2 = mock(TestBean.class);
		when(test2.getId()).thenReturn("id2");

		return new TestBean[] { test1, test2 };
	}
}
