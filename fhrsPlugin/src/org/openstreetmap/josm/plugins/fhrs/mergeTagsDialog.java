package org.openstreetmap.josm.plugins.fhrs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

public class mergeTagsDialog {

	public static Map<String, String> showTagsDialog (final Map<String, FHRSPlugin.oldAndNewValues> osmTags) {
		final JPanel panel = new JPanel();
		Object[][] columnValues = {};
		final List<Object[]> columnValuesList = new ArrayList<Object[]>();
		final List<String> tagList = new ArrayList<>(osmTags.keySet());
		final Map<String, String> returnValues = new HashMap<String, String>();
		Collections.sort(tagList);
		for(final String osmTag : tagList) {
			final FHRSPlugin.oldAndNewValues oanv = osmTags.get(osmTag);
			if (oanv.newValue != "") columnValuesList.add(new Object[] { false, osmTag, oanv.newValue, oanv.oldValue } );
		}
		columnValues = columnValuesList.toArray(columnValues);
		final osmTagsTableModel thisTableModel = new osmTagsTableModel();
		thisTableModel.data = columnValues;
		final JTable osmTagsTable = new JTable(thisTableModel);
		osmTagsTable.setRowHeight(20);
		osmTagsTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
		osmTagsTable.getColumnModel().getColumn(1).setPreferredWidth(200);
		osmTagsTable.getColumnModel().getColumn(2).setPreferredWidth(200);
		osmTagsTable.getColumnModel().getColumn(3).setPreferredWidth(200);
		panel.setLayout(new BorderLayout());
		panel.add(osmTagsTable.getTableHeader(), BorderLayout.PAGE_START);
		panel.add(osmTagsTable, BorderLayout.CENTER);
		final int result = JOptionPane.showOptionDialog(null, panel, "Select which values to merge from FHRS to OSM",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
				null, null, null);
		if (result == JOptionPane.OK_OPTION){
			for(int row = 0; row < osmTagsTable.getRowCount(); row++) {
				if ((Boolean)osmTagsTable.getValueAt(row, 0)) {
					returnValues.put(osmTagsTable.getValueAt(row, 1).toString(), osmTagsTable.getValueAt(row, 2).toString());
				}
			}
			return returnValues;
		} else {
			return null;
		}
	}

	static class osmTagsTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		String[] columnNames = { "Merge", "Tag", "New value", "Old Value" };
		public Object[][] data;

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
			return data.length;
		}

		@Override
		public String getColumnName(final int col) {
			return columnNames[col];
		}

		@Override
		public Object getValueAt(final int row, final int col) {
			return data[row][col];
		}

		@Override
		public Class getColumnClass(final int c) {
			return getValueAt(0, c).getClass();
		}

		/*
			* Don't need to implement this method unless your table's
			* editable.
			*/
		@Override
		public boolean isCellEditable(final int row, final int col) {
			return (col == 0 || col == 2);
		}

		/*
			* Don't need to implement this method unless your table's
			* data can change.
			*/
		@Override
			public void setValueAt(final Object value, final int row, final int col) {
			data[row][col] = value;
			fireTableCellUpdated(row, col);
		}
	}
}