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
package com.github.nfalco79.junit4osgi.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableColumn;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.osgi.service.log.LogService;

import com.github.nfalco79.junit4osgi.gui.runner.remote.JVMUtils;
import com.github.nfalco79.junit4osgi.gui.runner.remote.VirtualMachineDetails;
import com.github.nfalco79.junit4osgi.registry.spi.TestBean;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistry;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;

/**
 * Swing Runner for JUnit4OSGi registry.
 */
public class SwingRunner extends JFrame {

	private static final long serialVersionUID = 1L;

	/**
	 * State variable describing if we are executing tests.
	 */
	private boolean running = false;
	/**
	 * State variable describing if you intend stop the current test execution.
	 */
	private boolean stopped = false;

	private javax.swing.JButton btnRefresh;
	private javax.swing.JButton btnSearch;
	private javax.swing.JButton btnSelectAll;
	private javax.swing.JButton btnExecute;
	private javax.swing.JButton btnStop;
	private javax.swing.JLabel lblExecutedResults;
	private javax.swing.JTextArea messageArea;
	private javax.swing.JButton btnOk;
	private javax.swing.JProgressBar progressBar;
	private javax.swing.JDialog dlgTestResult;
	private javax.swing.JScrollPane srcResult;
	private javax.swing.JTable tblTestResult;
	private javax.swing.JList lstSuite;
	private javax.swing.JTextField txtSearchTest;
	private javax.swing.JMenuBar menuBar;
	private javax.swing.JPopupMenu popmnuTestSelected;
	private DefaultListModel/*<TestModel>*/ lstModel = new DefaultListModel/*<TestModel>*/();

	private transient SwingTestRegistryChangeListener registryListener = new SwingTestRegistryChangeListener();
	private transient TestRegistry registry;
	private transient LogService logService;


	public SwingRunner() {
		running = false;
	}

	private void internalInitComponents() {
		initComponents();
	}

	/**
	 * Start method.
	 */
	public void start() {
		internalInitComponents();
		setVisible(true);
		dlgTestResult.setVisible(false);
		refreshSuites();
		lblExecutedResults.setText(" \t No executed tests");
		progressBar.setMaximum(100);
		progressBar.setValue(100);

		TableColumn column = null;
		for (int i = 0; i < tblTestResult.getColumnCount(); i++) {
			column = tblTestResult.getColumnModel().getColumn(i);
			if (i == 0) {
				column.setPreferredWidth(350); // first column is bigger
			} else {
				column.setPreferredWidth(50);
				column.setCellRenderer(new ResultCellRenderer());
			}
		}
	}

	/**
	 * Stop method.
	 */
	public void stop() {
		if (registry != null && registryListener != null) {
			registry.removeTestRegistryListener(registryListener);
		}
		setVisible(false);
		dispose();
	}

	/**
	 * Refresh the list of available test suites.
	 */
	private void refreshSuites() {
		List<Object> selection = Arrays.asList(lstSuite.getSelectedValues());
		List<Integer> selectionIndexes = new ArrayList<Integer>(selection.size());

		SearchPattern searchPattern = new SearchPattern(txtSearchTest.getText());
		Set<TestBean> tests = registry.getTests();

		lstModel.clear();

		int index = 0;
		for (TestBean test : tests) {
			String text = test.getName().toLowerCase();
			if (searchPattern.matches(text)) {
				TestModel testModel = new TestModel(test);
				lstModel.addElement(testModel);
				if (selection.contains(testModel)) {
					selectionIndexes.add(index);
				}
				index++;
			}
		}

		for (Integer idx : selectionIndexes) {
			lstSuite.addSelectionInterval(idx, idx);
		}
	}

	/*
	 * Generated code.
	 */
	private void initComponents() {
		menuBar = new JMenuBar();
		dlgTestResult = new javax.swing.JDialog();
		JScrollPane srcMessage = new javax.swing.JScrollPane();
		messageArea = new javax.swing.JTextArea();
		btnOk = new javax.swing.JButton();
		JPanel panStatusBar = new javax.swing.JPanel();
		progressBar = new javax.swing.JProgressBar();
		btnExecute = new javax.swing.JButton();
		btnStop = new javax.swing.JButton();
		btnSelectAll = new javax.swing.JButton();
		JScrollPane srcSuite = new javax.swing.JScrollPane();
		lstSuite = new javax.swing.JList();
		srcResult = new javax.swing.JScrollPane();
		tblTestResult = new javax.swing.JTable() {
			@Override
			public Point getPopupLocation(MouseEvent event) {
				if (getSelectedRows().length == 0) {
					return null;
				}
				return super.getPopupLocation(event);
			}
		};
		lblExecutedResults = new javax.swing.JLabel();
		txtSearchTest = new javax.swing.JTextField();
		btnSearch = new javax.swing.JButton();
		btnRefresh = new javax.swing.JButton();

		JMenu mnuConnectTo = new JMenu("Connect to");
		mnuConnectTo.getAccessibleContext().setAccessibleDescription("Allow to connect a JVM where a juni4osgi runner is started");
		menuBar.add(mnuConnectTo);

		JMenuItem mnuLocalJVM = new JMenuItem("Local JMV");
		mnuLocalJVM.getAccessibleContext().setAccessibleDescription("Connect the same JVM of this runner by declarative OSGi service");
		mnuLocalJVM.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// get OSGi declared services
			}
		});
		mnuConnectTo.add(mnuLocalJVM);

		JMenu mnuRemoteJVM = new JMenu("Remote JVM");
		mnuRemoteJVM.getAccessibleContext().setAccessibleDescription("Allow to connect a JVM by JMX");
		mnuConnectTo.add(mnuRemoteJVM);

		JMenuItem mnuItmLocalJVM = new JMenuItem("localhost");
		mnuItmLocalJVM.getAccessibleContext().setAccessibleDescription("Allow to connect a JVM on localhost by JMX");
		mnuItmLocalJVM.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<VirtualMachineDetails> listVMs = JVMUtils.listVMs();
				// show a dialog where choose which JVM on localhost to connect
			}
		});
		mnuRemoteJVM.add(mnuItmLocalJVM);

		JMenuItem mnuItmURLJVM = new JMenuItem("JMX URL");
		mnuItmURLJVM.getAccessibleContext().setAccessibleDescription("Allow to connect JVM by a JMX url");
		mnuItmURLJVM.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// show a dialog to insert the JMX URL to connect to
			}
		});
		mnuRemoteJVM.add(mnuItmURLJVM);

		setJMenuBar(menuBar);

		dlgTestResult.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		dlgTestResult.setMinimumSize(new Dimension(320, 250));
		dlgTestResult.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent evt) {
				onDialogClosed(evt);
			}

			@Override
			public void windowClosing(WindowEvent evt) {
				onDialogClosed(evt);
			}
		});

		srcMessage.setBorder(null);
		srcMessage.setMinimumSize(new Dimension(300, 202));
		srcMessage.setPreferredSize(new Dimension(300, 202));
		srcMessage.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		srcMessage.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		messageArea.setBackground(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
		messageArea.setColumns(20);
		messageArea.setEditable(false);
		messageArea.setLineWrap(true);
		messageArea.setRows(5);
		messageArea.setWrapStyleWord(true);
		srcMessage.setViewportView(messageArea);

		dlgTestResult.getContentPane().add(srcMessage, java.awt.BorderLayout.CENTER);

		btnOk.setText("Ok");
		btnOk.setPreferredSize(new Dimension(120, 23));
		btnOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				okActionPerformed(evt);
			}
		});
		dlgTestResult.getContentPane().add(btnOk, java.awt.BorderLayout.SOUTH);

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("JUnit4OSGi Runner");
		setMinimumSize(null);
		setPreferredSize(new Dimension(622, 657));

		progressBar.setMinimumSize(null);
		progressBar.setPreferredSize(null);

		javax.swing.GroupLayout lyStatusBar = new GroupLayout(panStatusBar);
		panStatusBar.setLayout(lyStatusBar);
		lyStatusBar.setHorizontalGroup(lyStatusBar.createParallelGroup(Alignment.LEADING).addComponent(progressBar,
				GroupLayout.DEFAULT_SIZE, 466, Short.MAX_VALUE));
		lyStatusBar.setVerticalGroup(lyStatusBar.createParallelGroup(Alignment.LEADING)
				.addGroup(lyStatusBar.createSequentialGroup()
						.addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		btnExecute.setText("Execute");
		btnExecute.setEnabled(!running);
		btnExecute.setToolTipText("Execute selected testcase/s");
		btnExecute.setMaximumSize(new Dimension(90, 23));
		btnExecute.setMinimumSize(new Dimension(90, 23));
		btnExecute.setPreferredSize(new Dimension(100, 23));
		btnExecute.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				executeButtonActionPerformed(evt);
			}
		});

		btnStop.setText("Stop");
		btnStop.setEnabled(stopped);
		btnStop.setToolTipText("Stop current execution of selected testcase/s");
		btnStop.setMaximumSize(new Dimension(90, 23));
		btnStop.setMinimumSize(new Dimension(90, 23));
		btnStop.setPreferredSize(new Dimension(100, 23));
		btnStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				stopButtonActionPerformed(evt);
			}
		});

		btnSelectAll.setText("Select All");
		btnSelectAll.setToolTipText("Select all testcases");
		btnSelectAll.setMaximumSize(new Dimension(90, 23));
		btnSelectAll.setMinimumSize(new Dimension(90, 23));
		btnSelectAll.setPreferredSize(new Dimension(100, 23));
		btnSelectAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				allButtonActionPerformed(evt);
			}
		});

		srcSuite.setAutoscrolls(true);

		lstSuite.setModel(lstModel);
		lstSuite.setMaximumSize(null);
		lstSuite.setMinimumSize(null);
		lstSuite.setPreferredSize(null);
		srcSuite.setViewportView(lstSuite);

		srcResult.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		tblTestResult.setAutoCreateRowSorter(true);
		tblTestResult.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
		tblTestResult.setModel(new ResultTableModel());
		tblTestResult.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		tblTestResult.setMaximumSize(null);
		tblTestResult.setMinimumSize(null);
		tblTestResult.setPreferredSize(null);
		tblTestResult.getTableHeader().setReorderingAllowed(false);
		tblTestResult.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent evt) {
//				showPopupMenu(evt);
			}

			@Override
			public void mouseClicked(MouseEvent evt) {
			    showPopupMenu(evt);
				resultTableMouseClicked(evt);
			}
		});
		srcResult.setViewportView(tblTestResult);

		JMenuItem mnuRelaunch = new JMenuItem("Relaunch");
		mnuRelaunch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				relaunchPopUpMenuActionPerformed(evt);
			}
		});
		popmnuTestSelected = new JPopupMenu();
		popmnuTestSelected.add(mnuRelaunch);

		lblExecutedResults.setPreferredSize(null);

		txtSearchTest.setToolTipText("Search your testcase by typing part of its name");

		btnSearch.setText("Search");
		btnSearch.setToolTipText("search testcase by its name");
		btnSearch.setMaximumSize(new Dimension(90, 23));
		btnSearch.setMinimumSize(new Dimension(90, 23));
		btnSearch.setPreferredSize(new Dimension(100, 23));
		btnSearch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				btnSearchActionPerformed(evt);
			}
		});

		btnRefresh.setToolTipText("Force refresh of testcase list");
		btnRefresh.setText("Refresh");
		btnRefresh.setPreferredSize(new Dimension(90, 23));
		btnRefresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				btnRefreshActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(layout.createSequentialGroup()
				.addContainerGap().addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(srcResult,
						Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 598, Short.MAX_VALUE)
						.addGroup(
								layout.createSequentialGroup()
										.addComponent(panStatusBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(lblExecutedResults, GroupLayout.PREFERRED_SIZE,
												GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGroup(Alignment.TRAILING, layout
								.createSequentialGroup()
								.addGroup(layout.createParallelGroup(Alignment.TRAILING)
										.addComponent(txtSearchTest, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.addComponent(srcSuite, GroupLayout.DEFAULT_SIZE, 492, Short.MAX_VALUE))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(layout.createParallelGroup(Alignment.LEADING, false)
										.addComponent(btnExecute, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.addComponent(btnStop, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.addComponent(btnSelectAll, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.addComponent(btnSearch, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.addComponent(btnRefresh, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE))))
				.addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addGroup(layout.createParallelGroup(Alignment.BASELINE)
								.addComponent(txtSearchTest, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(btnSearch, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(layout.createParallelGroup(Alignment.LEADING)
								.addGroup(layout.createSequentialGroup()
										.addComponent(btnSelectAll, GroupLayout.PREFERRED_SIZE,
												GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(btnExecute, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addComponent(btnStop, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(btnRefresh, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addGap(0, 0, Short.MAX_VALUE))
								.addComponent(srcSuite, GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(srcResult, GroupLayout.PREFERRED_SIZE, 374, GroupLayout.PREFERRED_SIZE)
						.addGap(18, 18, 18)
						.addGroup(layout.createParallelGroup(Alignment.LEADING)
								.addComponent(lblExecutedResults, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(panStatusBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))));

		pack();
	}

	private void relaunchPopUpMenuActionPerformed(ActionEvent evt) {
		int[] selection = tblTestResult.getSelectedRows();
		if (running || selection.length == 0) {
			return;
		}

		running = true;

		btnExecute.setEnabled(false);
		btnExecute.setText("Running...");
		btnStop.setEnabled(true);

		List<Description> list = new ArrayList<Description>(selection.length);
		ResultTableModel model = (ResultTableModel) tblTestResult.getModel();
		for (int idx : selection) {
			int idxModel = tblTestResult.convertRowIndexToModel(idx);
			list.add(model.getTest(idxModel));
		}
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		executeTestDescription(list);
	}

	private void executeButtonActionPerformed(ActionEvent evt) {
		if (running) {
			return;
		}

		Object[] selection = lstSuite.getSelectedValues();
//		List selection = lstSuite.getSelectedValuesList(); java 7

		// Collect selected test.
		List<TestBean> list = new ArrayList<TestBean>(selection.length);
		for (Object selected : selection) {
			if (selected != null) {
				list.add(((TestModel) selected).getTest());
			}
		}

		if (list.isEmpty()) {
			return;
		}

		running = true;

		btnExecute.setEnabled(false);
		btnExecute.setText("Running...");
		btnStop.setEnabled(true);

		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		executeTest(list);
	}

	private void stopButtonActionPerformed(ActionEvent evt) {
		if (!running || stopped) {
			return;
		}
		btnStop.setEnabled(false);
		btnStop.setText("Stopping...");
		stopped = true;
	}

	private void allButtonActionPerformed(ActionEvent evt) {
		int max = lstModel.getSize();
		int[] indices = new int[max];
		for (int i = 0; i < max; i++) {
			indices[i] = i;
		}
		lstSuite.setSelectedIndices(indices);
	}

	private void showPopupMenu(MouseEvent evt) {
		if (evt.isPopupTrigger() || isRightMouseButton(evt)) {
			if (tblTestResult.getSelectionModel().isSelectionEmpty()) {
				tblTestResult.setComponentPopupMenu(null);
			} else {
				popmnuTestSelected.show(evt.getComponent(), evt.getX(), evt.getY());
			}
		}
	}

	private void resultTableMouseClicked(MouseEvent evt) {
		if (isLeftMouseButton(evt) && evt.getClickCount() == 2 && !evt.isConsumed()) {
			evt.consume();

			Point p = evt.getPoint();
			int row = tblTestResult.convertRowIndexToModel(tblTestResult.rowAtPoint(p));
			int col = tblTestResult.convertColumnIndexToModel(tblTestResult.columnAtPoint(p));
			ResultTableModel model = (ResultTableModel) tblTestResult.getModel();
			String message = model.getMessage(row, col);
			if (message != null) {
				setEnabled(false);
				dlgTestResult.setTitle("Test Report");
				messageArea.setText(message);
				dlgTestResult.setVisible(true);
			}
		}
	}

	private void okActionPerformed(ActionEvent evt) {
		dlgTestResult.setVisible(false);
		setEnabled(true);
	}

	private void btnRefreshActionPerformed(ActionEvent evt) {
		refreshSuites();
	}

	private void onDialogClosed(WindowEvent evt) {
		dlgTestResult.setVisible(false);
		setEnabled(true);
	}

	private void btnSearchActionPerformed(ActionEvent evt) {
		refreshSuites();

	}

	/**
	 * Execute test methods.
	 *
	 * @param descriptions
	 *            of test to execute.
	 */
	private void executeTestDescription(final List<Description> descriptions) {
		progressBar.setIndeterminate(true);

		ResultTableModel model = (ResultTableModel) tblTestResult.getModel();
		model.clear();

		Runnable thread = new TestRunnable<Description>(descriptions) {

			@Override
			protected String getTestMethod(Description test) {
				return test.getMethodName();
			}

			@Override
			protected Class<?> getTestClass(Description test) {
				return test.getTestClass();
			}
		};

		new Thread(thread).start();
	}

	/**
	 * Execute test suites.
	 *
	 * @param tests
	 *            bean OSGi wrapper of test suite class to execute.
	 */
	private void executeTest(final List<TestBean> tests) {
		progressBar.setIndeterminate(true);

		ResultTableModel model = (ResultTableModel) tblTestResult.getModel();
		model.clear();

		Runnable thread = new TestRunnable<TestBean>(tests) {
			@Override
			protected String getTestMethod(TestBean test) {
				return null;
			}

			@Override
			protected Class<?> getTestClass(TestBean test) {
				try {
					return test.getTestClass();
				} catch (ClassNotFoundException e) {
					// skip test
					if (logService != null) {
						logService.log(LogService.LOG_ERROR, "Skip test " + test.getName(), e);
					}
				} catch (NoClassDefFoundError e) {
					// skip test
					if (logService != null) {
						logService.log(LogService.LOG_ERROR, "Skip test " + test.getName(), e);
					}
				}
				return null;
			}
		};

		new Thread(thread).start();
	}

	/**
	 * Compute executed tests. (Status bar message)
	 */
	private void computeExecutedTest() {
		ResultTableModel results = (ResultTableModel) tblTestResult.getModel();
		String m = " \t ";
		m += results.getTestCount() + " tests executed / ";
		m += results.getSuccess() + " success / ";
		m += results.getFailures() + " failures / ";
		m += results.getErrors() + " errors ";
		lblExecutedResults.setText(m);
	}

	public void setRegistry(TestRegistry registry) {
		this.registry = registry;
		registry.addTestRegistryListener(registryListener);
	}

	public void setLog(LogService logService) {
		if (logService == this.logService) {
			this.logService = null;
		} else {
			this.logService = logService;
		}
	}

    /**
     * Returns true if the mouse event specifies the left mouse button.
     *
     * @param anEvent
     *            a MouseEvent object
     * @return true if the left mouse button was active
     */
    public static boolean isLeftMouseButton(MouseEvent anEvent) {
        return ((anEvent.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0 || anEvent.getButton() == MouseEvent.BUTTON1);
    }

    /**
     * Returns true if the mouse event specifies the right mouse button.
     *
     * @param anEvent
     *            a MouseEvent object
     * @return true if the right mouse button was active
     */
    public static boolean isRightMouseButton(MouseEvent anEvent) {
        return ((anEvent.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0 || anEvent.getButton() == MouseEvent.BUTTON3);
    }

	private abstract class TestRunnable<T> implements Runnable {
		private List<T> tests;

		public TestRunnable(List<T> tests) {
			this.tests = tests;
		}

		@Override
		public void run() {
			JUnitCore core = new JUnitCore();
			core.addListener(new MyTestListener());

			Iterator<T> testIt = tests.iterator();
			while (!stopped && testIt.hasNext()) {
				T test = testIt.next();
				Class<?> testClass = getTestClass(test);
				String methodName = getTestMethod(test);

				if (methodName == null) {
					core.run(testClass);
				} else {
					core.run(Request.method(testClass, methodName));
				}
			}

			progressBar.setIndeterminate(false);
			progressBar.setMaximum(100);
			progressBar.setValue(100);

			btnExecute.setText("Execute");
			btnExecute.setEnabled(true);
			btnStop.setText("Stop");
			btnStop.setEnabled(false);
			running = false;
			stopped = false;

			computeExecutedTest();
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}

		protected abstract String getTestMethod(T test);

		protected abstract Class<?> getTestClass(T test);
	}

	private class MyTestListener extends RunListener {
		/**
		 * Table model.
		 */
		private ResultTableModel tblModel = (ResultTableModel) tblTestResult.getModel();

		@Override
		public void testIgnored(Description description) throws Exception {
			tblModel.addSkippedTest(description);
			adjustScroll();
		}

		@Override
		public void testAssumptionFailure(Failure failure) {
			tblModel.addFailedTest(failure.getDescription(), (Error) failure.getException());
			adjustScroll();
		}

		@Override
		public void testFailure(Failure failure) throws Exception {
			if (failure.getException() instanceof Error) {
				tblModel.addFailedTest(failure.getDescription(), (Error) failure.getException());
			} else {
				tblModel.addErrorTest(failure.getDescription(), failure.getException());
			}
			adjustScroll();
		}

		@Override
		public void testFinished(Description description) throws Exception {
			tblModel.addTest(description);
			adjustScroll();
		}

		/**
		 * Adjust the scrolling bar of the result table.
		 */
		private void adjustScroll() {
			JScrollBar bar = srcResult.getVerticalScrollBar();
			if ((bar != null) && (bar.isVisible())) {
				bar.setValue(Integer.MAX_VALUE);
			}
		}
	}

	private class SwingTestRegistryChangeListener implements TestRegistryChangeListener {
		@Override
		public void registryChanged(TestRegistryEvent event) {
			TestModel testModel = new TestModel(event.getTest());

			switch (event.getType()) {
			case ADD:
				SearchPattern searchPattern = new SearchPattern(txtSearchTest.getText());
				String testName = testModel.toString().toLowerCase();
				if (searchPattern.matches(testName)) {
					lstModel.addElement(testModel);
				}
				break;
			case REMOVE:
				lstModel.removeElement(testModel);
				break;
			default:
				break;
			}
		}
	}
}