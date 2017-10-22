/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
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
package com.github.nfalco79.junit4osgi.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;

import com.github.nfalco79.junit4osgi.registry.spi.TestBean;

/**
 * Test Suite list model.
 */
public class TestListModel extends AbstractListModel {

	private static final long serialVersionUID = 1L;

	/**
	 * List of {@link TestRecord}.
	 */
	private List<TestRecord> list = new ArrayList<TestRecord>();

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.ListModel#getElementAt(int)
	 */
	@Override
	public Object getElementAt(int index) {
		if (index >= list.size()) {
			return null;
		} else {
			return list.get(index).toString();
		}
	}

	/**
	 * Gets the test object placed at the given index.
	 *
	 * @param index
	 *            the index
	 * @return the test object placed at the given index
	 */
	public TestBean getTestElementAt(int index) {
		return list.get(index).getTest();
	}

	/**
	 * Adds a test.
	 *
	 * @param test
	 *            the test to add
	 */
	public void addTest(TestBean test) {
		boolean fireEvent;
		synchronized (this) {
			TestRecord tr = new TestRecord(test);
			if (!list.contains(tr)) {
				fireEvent = true;
				list.add(tr);
			} else {
				fireEvent = false;
			}
		}
		if (fireEvent) {
			fireContentsChanged(this, list.size() - 1, list.size() - 1);
		}
	}

	/**
	 * Removes a test.
	 *
	 * @param test
	 *            the test to remove
	 */
	public void removeTest(TestBean test) {
		int index = 1;
		synchronized (this) {
			for (TestRecord t : list) {
				if (t.getTest().equals(test)) {
					index = list.indexOf(t);
					list.remove(t);
					return;
				}
			}
		}

		if (index != -1) {
			fireContentsChanged(this, index, index);
		}
	}

	/**
	 * Clears the list.
	 */
	public void clear() {
		list.clear();
	}

	/**
	 * Gets the list size.
	 *
	 * @return the list size.
	 * @see javax.swing.ListModel#getSize()
	 */
	@Override
	public int getSize() {
		return list.size();
	}

}