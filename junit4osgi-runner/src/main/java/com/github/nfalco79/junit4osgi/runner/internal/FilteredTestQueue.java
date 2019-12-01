/*
 * Copyright 2019 Nikolas Falco
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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;

public class FilteredTestQueue implements Queue<TestBean> {

    private final TestFilter filter;
    private final ConcurrentLinkedQueue<TestBean> queue;

    public FilteredTestQueue(TestFilter filter) {
        this.filter = filter == null ? new TestFilter(null, null) : filter;
        this.queue = new ConcurrentLinkedQueue<TestBean>();
    }

    @Override
    public int hashCode() {
        return queue.hashCode();
    }

    @Override
    public TestBean remove() {
        return queue.remove();
    }

    @Override
    public boolean equals(Object obj) {
        return queue.equals(obj);
    }

    @Override
    public TestBean element() {
        return queue.element();
    }

    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return queue.containsAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return queue.removeAll(c);
    }

    @Override
    public boolean offer(TestBean test) {
        if (filter.accept(test.getName())) {
            return queue.offer(test);
        }
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return queue.retainAll(c);
    }

    @Override
    public TestBean poll() {
        return queue.poll();
    }

    @Override
    public TestBean peek() {
        return queue.peek();
    }

    @Override
    public String toString() {
        return queue.toString();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean contains(Object o) {
        return queue.contains(o);
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(o);
    }

    @Override
    public boolean addAll(Collection<? extends TestBean> tests) {
        Set<TestBean> filtered = new LinkedHashSet<TestBean>(tests.size());
        for (TestBean testBean : tests) {
            if (filter.accept(testBean.getName())) {
                filtered.add(testBean);
            }
        }
        return queue.addAll(filtered);
    }

    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return queue.toArray(a);
    }

    @Override
    public Iterator<TestBean> iterator() {
        return queue.iterator();
    }

    @Override
    public boolean add(TestBean testBean) {
        if (filter.accept(testBean.getName())) {
            return queue.add(testBean);
        }
        return false;
    }

    public void remove(TestBean testBean) {
        queue.remove();
    }

}