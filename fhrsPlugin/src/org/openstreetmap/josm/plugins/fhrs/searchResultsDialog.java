package org.openstreetmap.josm.plugins.fhrs;

import java.util.ArrayList;
import java.util.List;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

public class searchResultsDialog {

	public static String showSearchDialog (List<List<String>> searchResults) {
		JPanel panel = new JPanel();
		Object[][] columnValues = {};
		List<Object[]> columnValuesList = new ArrayList<Object[]>();
		String returnValue = "";

		for(List<String> searchResult: searchResults) {
			columnValuesList.add(new Object[] { 
				searchResult.get(0).toString(), 
				searchResult.get(1).toString(), 
				searchResult.get(2).toString() 
			} );
		}
		columnValues = columnValuesList.toArray(columnValues);
		searchResultsTableModel thisTableModel = new searchResultsTableModel();
		thisTableModel.data = columnValues;
		JTable searchResultsTable = new JTable(thisTableModel);
		searchResultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		searchResultsTable.setRowHeight(20);
		searchResultsTable.getColumnModel().getColumn(0).setPreferredWidth(100);
		searchResultsTable.getColumnModel().getColumn(1).setPreferredWidth(200);
		searchResultsTable.getColumnModel().getColumn(2).setPreferredWidth(400);
		panel.setLayout(new BorderLayout());
		panel.add(searchResultsTable.getTableHeader(), BorderLayout.PAGE_START);
		panel.add(searchResultsTable, BorderLayout.CENTER);
		int result = JOptionPane.showOptionDialog(null, panel, "Select which Business's data you want to complete from FHRS",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
				null, null, null);
		if (result == JOptionPane.OK_OPTION){
			int iRow = searchResultsTable.getSelectedRow();
			returnValue = searchResultsTable.getValueAt(iRow, 0).toString();
			return returnValue;
		} else {
			return null;
		}
	}

	static class searchResultsTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		String[] columnNames = { "FHRS ID", "Business Name", "Address" };
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
		public String getColumnName(int col) {
			return columnNames[col];
		}

		@Override
		public Object getValueAt(int row, int col) {
			return data[row][col];
		}

		@Override
		public Class getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return false;
		}

		@Override
			public void setValueAt(Object value, int row, int col) {
			data[row][col] = value;
			fireTableCellUpdated(row, col);
		}
	}
}