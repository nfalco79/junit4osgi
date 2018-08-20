package com.github.nfalco79.junit4osgi.gui;

import javax.swing.DefaultListModel;

import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryChangeListener;
import com.github.nfalco79.junit4osgi.registry.spi.TestRegistryEvent;

@SuppressWarnings("rawtypes")
/*package*/ class SwingTestRegistryChangeListener implements TestRegistryChangeListener {
	private final DefaultListModel/*<TestModel>*/ lstModel;
	private String filterText;

	SwingTestRegistryChangeListener(final DefaultListModel/*<TestModel>*/ lstModel, final String filter) {
		this.lstModel = lstModel;
		this.filterText = filter;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void registryChanged(TestRegistryEvent event) {
		if (lstModel == null) {
			return;
		}

		TestModel testModel = new TestModel(event.getTest().getId());

		switch (event.getType()) {
		case ADD:
			SearchPattern searchPattern = new SearchPattern(filterText);
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

	public void setFilter(String filterText) {
		this.filterText = filterText;
	}
}