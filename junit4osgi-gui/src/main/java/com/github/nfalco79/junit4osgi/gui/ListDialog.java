package com.github.nfalco79.junit4osgi.gui;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;

import com.github.nfalco79.junit4osgi.gui.internal.runner.remote.VirtualMachineDetails;

@SuppressWarnings("serial")
/* package */ class ListDialog<E> extends JDialog {

	private DefaultListModel<E> dataModel;
	private E selection;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ListDialog() {
		super();

		dataModel = new DefaultListModel<E>();
		JList list = new JList(dataModel);
		list.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				VirtualMachineDetails vm = (VirtualMachineDetails) value;
				setText(vm.getDescription());
				setToolTipText(vm.getJmxURL());
				return c;
			}
		});
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				JList list = (JList) evt.getSource();
				if (evt.getClickCount() >= 2) {
					int index = list.locationToIndex(evt.getPoint());
					selection = (E) list.getModel().getElementAt(index);
					ListDialog.this.dispose();
				}
			}
		});
		add(list);
	}

	public void fillWith(Collection<E> elements) {
		for (E element : elements) {
			dataModel.addElement(element);
		}
	}

	public E getSelection() {
		return selection;
	}
}