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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.junit.Ignore;
import org.junit.runner.Description;

/**
 * Result Table Model. Store the results of executed tests.
 */
public class ResultTableModel extends AbstractTableModel {

	/**
	 * Success String.
	 */
	public static final String SUCCESS = "success";

	/**
	 * Failure String.
	 */
	public static final String FAILURE = "failure";

	/**
	 * Error String.
	 */
	public static final String ERROR = "error";

	/**
	 * Skipped String.
	 */
	public static final String SKIPPED = "skip";

	private static final long serialVersionUID = 1L;

	/**
	 * List of results.
	 */
	private List<TestRecord> results = new ArrayList<TestRecord>();

	@Override
	public int getRowCount() {
		return results.size();
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	/**
	 * Adds a failing test.
	 *
	 * @param t
	 *            the test
	 * @param e
	 *            the assertion error
	 */
	public void addFailedTest(Description t, Error e) {
		if (!contains(t)) {
			TestRecord rec = new FailureTestRecord(t, e);
			results.add(rec);
			fireTableDataChanged();
		}
	}

	/**
	 * Adds a test in error.
	 *
	 * @param t
	 *            the test
	 * @param e
	 *            the thrown error
	 */
	public void addErrorTest(Description t, Throwable e) {
		if (!contains(t)) {
			TestRecord rec = new ErrorTestRecord(t, e);
			results.add(rec);
			fireTableDataChanged();
		}
	}

	/**
	 * Adds a skipped test.
	 *
	 * @param t
	 *            the test description
	 */
	public void addSkippedTest(Description t) {
		if (!contains(t)) {
			TestRecord rec = new SkippedTestRecord(t);
			results.add(rec);
			fireTableDataChanged();
		}
	}

	/**
	 * Adds a sucessfull test.
	 *
	 * @param t
	 *            the test
	 */
	public void addTest(Description t) {
		if (!contains(t)) {
			TestRecord rec = new TestRecord(t);
			results.add(rec);
			fireTableDataChanged();
		}
	}

	public int getTestCount() {
		return results.size();
	}

	/**
	 * Gets the number of success.
	 *
	 * @return the number of success
	 */
	public int getSucess() {
		int count = 0;
		for (TestRecord test : results) {
			if (test.isSucess()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Gets the number of errors.
	 *
	 * @return the number of errors
	 */
	public int getErrors() {
		int count = 0;
		for (TestRecord test : results) {
			if (test.isError()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Gets the number of failures.
	 *
	 * @return the number of failures
	 */
	public int getFailures() {
		int count = 0;
		for (TestRecord test : results) {
			if (test.isFailed()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Does the result list contains the given test.
	 *
	 * @param t
	 *            the test
	 * @return {@code true} if the list contains the test.
	 */
	private boolean contains(Description t) {
		for (TestRecord test : results) {
			if (test.getTest().equals(t)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Clear the list.
	 */
	public void clear() {
		results.clear();
		fireTableDataChanged();
	}

	/**
	 * Get the Object placed in the JTable.
	 *
	 * @param rowIndex
	 *            the row
	 * @param columnIndex
	 *            the column
	 * @return the object
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == 0) {
			return results.get(rowIndex).getTest();
		}
		if (columnIndex == 1) {
			TestRecord tr = results.get(rowIndex);
			if (tr.isSucess()) {
				return SUCCESS;
			}
			if (tr instanceof FailureTestRecord) {
				return FAILURE;
			}
			if (tr instanceof ErrorTestRecord) {
				return ERROR;
			}
			if (tr instanceof SkippedTestRecord) {
				return SKIPPED;
			}
		}
		return null;
	}

	/**
	 * Gets column names.
	 *
	 * @param column
	 *            the column
	 * @return the column name
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(int column) {
		if (column == 0) {
			return "Test";
		}

		if (column == 1) {
			return "Status";
		}

		return null;
	}

	/**
	 * Gets the message.
	 *
	 * @param row
	 *            the row
	 * @param column
	 *            the column
	 * @return the message for this cell
	 */
	public String getMessage(int row, int column) {
		if (row == -1) {
			return null;
		}
		TestRecord rec = results.get(row);
		if (rec.isSucess()) {
			return "The test " + rec.getTest() + " was executed sucessfully.";
		} else if (rec.isSkipped()) {
			return "The test " + rec.getTest() + " was ignored.";
		} else if (rec.isFailed()) {
			Error failure = ((FailureTestRecord) rec).getFailure();
			return "The test " + rec.getTest() + " has failed : \n" + failure.getMessage();
		} else if (rec.isError()) {
			Throwable error = ((ErrorTestRecord) rec).getError();
			String message = "The test " + rec.getTest() + " has thrown an error : \n" + error.getMessage();
			StringWriter sw = new StringWriter();
			error.printStackTrace(new PrintWriter(sw));
			message += "\n" + sw.toString();
			return message;
		}
		return "";
	}

	private class ErrorTestRecord extends TestRecord {
		private Throwable error;

		/**
		 * Creates a TestRecord.
		 *
		 * @param description
		 *            the test description
		 * @param t
		 *            the cause of failure
		 */
		public ErrorTestRecord(Description description, Throwable t) {
			super(description);
			error = t;
		}

		/**
		 * Returns the unexpected failure cause o the running test case.
		 *
		 * @return the cause exception.
		 */
		public Throwable getError() {
			return error;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.github.nfalco79.junit4osgi.gui.ResultTableModel.TestRecord#
		 * isError()
		 */
		@Override
		public boolean isError() {
			return true;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.github.nfalco79.junit4osgi.gui.ResultTableModel.TestRecord#
		 * isSucess()
		 */
		@Override
		public boolean isSucess() {
			return false;
		}
	}

	private class SkippedTestRecord extends TestRecord {
		/**
		 * Creates a TestRecord.
		 *
		 * @param description
		 *            the test description
		 */
		public SkippedTestRecord(Description description) {
			super(description);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.github.nfalco79.junit4osgi.gui.ResultTableModel.TestRecord#
		 * isSkipped()
		 */
		@Override
		public boolean isSkipped() {
			return true;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.github.nfalco79.junit4osgi.gui.ResultTableModel.TestRecord#
		 * isSucess()
		 */
		@Override
		public boolean isSucess() {
			return false;
		}
	}

	private class FailureTestRecord extends TestRecord {
		private Error failure;

		/**
		 * Creates a TestRecord.
		 *
		 * @param description
		 *            the test description
		 * @param e
		 *            the failure cause
		 */
		public FailureTestRecord(Description description, Error e) {
			super(description);
			failure = e;
		}

		/**
		 * Returns the failure of an assertion.
		 *
		 * @return assertion failure.
		 */
		public Error getFailure() {
			return failure;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.github.nfalco79.junit4osgi.gui.ResultTableModel.TestRecord#
		 * isFailed()
		 */
		@Override
		public boolean isFailed() {
			return true;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.github.nfalco79.junit4osgi.gui.ResultTableModel.TestRecord#
		 * isSucess()
		 */
		@Override
		public boolean isSucess() {
			return false;
		}
	}

	private class TestRecord {
		private Description test;

		/**
		 * Creates a TestRecord.
		 *
		 * @param description
		 *            the test description
		 */
		public TestRecord(Description description) {
			test = description;
		}

		/**
		 * Returns description of the running test.
		 *
		 * @return the test description
		 */
		public Description getTest() {
			return test;
		}

		/**
		 * If test was run with success.
		 *
		 * @return if test is passed.
		 */
		public boolean isSucess() {
			return true;
		}

		/**
		 * If test was skipped.
		 *
		 * @return is the test is marked with the {@link Ignore} annotation.
		 */
		public boolean isSkipped() {
			return false;
		}

		public boolean isFailed() {
			return false;
		}

		public boolean isError() {
			return false;
		}

	}
}