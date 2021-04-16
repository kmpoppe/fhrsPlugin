package org.openstreetmap.josm.plugins.fhrs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;

import org.openstreetmap.josm.plugins.fhrs.FHRSPlugin.fhrsAuthority;

public class mergeTagsDialog {

	public static Map<String, String> showTagsDialog (final Map<String, FHRSPlugin.oldAndNewValues> osmTags, boolean warnCapitalization, final Map<String, String> JsonToOSM) {
		final JPanel panel = new JPanel();
		Object[][] columnValues = {};
		final List<Object[]> columnValuesList = new ArrayList<Object[]>();
		final List<String> tagList = new ArrayList<>(osmTags.keySet());
		final Map<String, String> returnValues = new HashMap<String, String>();
		Collections.sort(tagList);
		String jsonDisplay = "";
		for(final String osmTag : tagList) {
			final FHRSPlugin.oldAndNewValues oanv = osmTags.get(osmTag);
			for(Map.Entry<String, String> entry : JsonToOSM.entrySet()) {
				if (entry.getValue() == osmTag) jsonDisplay = entry.getKey(); 
			}
			if (oanv.newValue != "") {
				columnValuesList.add(new Object[] { 
					false, 
					jsonDisplay,
					osmTag, 
					oanv.newValue, 
					oanv.oldValue 
				} );
			}
		}
		columnValues = columnValuesList.toArray(columnValues);
		final osmTagsTableModel thisTableModel = new osmTagsTableModel();
		thisTableModel.data = columnValues;
		final JTable osmTagsTable = new JTable(thisTableModel);
		osmTagsTable.setRowHeight(20);
		osmTagsTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
		osmTagsTable.getColumnModel().getColumn(2).setCellEditor(new CustomTableCellEditor());
		osmTagsTable.getColumnModel().getColumn(3).setCellEditor(new CustomTableCellEditor());
		osmTagsTable.getColumnModel().getColumn(1).setPreferredWidth(200);
		osmTagsTable.getColumnModel().getColumn(2).setPreferredWidth(200);
		osmTagsTable.getColumnModel().getColumn(3).setPreferredWidth(200);
		osmTagsTable.getColumnModel().getColumn(4).setPreferredWidth(200);

		TableRowSorter<osmTagsTableModel> sorter = new TableRowSorter<osmTagsTableModel>(thisTableModel);
		osmTagsTable.setRowSorter(sorter);
		List<RowSorter.SortKey> sortKeys = new ArrayList<>(25);
		sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
		sorter.setSortKeys(sortKeys);

		panel.setLayout(new BorderLayout());
		panel.add(osmTagsTable.getTableHeader(), BorderLayout.PAGE_START);
		panel.add(osmTagsTable, BorderLayout.CENTER);
		JPanel bottomMessages = new JPanel(new BorderLayout());
		bottomMessages.add(new JLabel("Select which values to merge from FHRS to OSM. You can also change the \"New value\" if you need to before merging."), "First");
		if (warnCapitalization) {
			bottomMessages.add(new JLabel("Warning! Some entries had wrong casing. I tried to figure out the correct casing, but the result might be wrong. Please check before merging!"), "Last");
		}
		panel.add(bottomMessages, BorderLayout.PAGE_END);
		final int result = JOptionPane.showOptionDialog(null, panel, "Select which values to merge from FHRS to OSM",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
				null, null, null);
		for(int row = 0; row < osmTagsTable.getRowCount(); row++) {
			if ((Boolean)osmTagsTable.getValueAt(row, 0)) {
				returnValues.put(osmTagsTable.getValueAt(row, 2).toString(), osmTagsTable.getValueAt(row, 3).toString());
			}
		}
		if (result == JOptionPane.OK_OPTION){
			if (returnValues.size() == 0) return null; else return returnValues;
		} else {
			if (returnValues.size() > 0 ) FHRSPlugin.msgBox("Setting values was cancelled!", JOptionPane.INFORMATION_MESSAGE);
			return null;
		}
	}
	public static class CustomTableCellEditor extends AbstractCellEditor implements TableCellEditor {
		private static final long serialVersionUID = 1L;
		private TableCellEditor editor;

        @Override
        public Object getCellEditorValue() {
            if (editor != null) {
                return editor.getCellEditorValue();
            }
            return null;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			editor = new DefaultCellEditor(new JTextField());
			if (column == 2) {
				JComboBox<String> oComboBox = new JComboBox<String>();
				oComboBox.addItem("addr:city");
				oComboBox.addItem("addr:housename");
				oComboBox.addItem("addr:housenumber");
				oComboBox.addItem("addr:street");
				oComboBox.addItem("addr:unit");
				oComboBox.setSelectedItem(value.toString());
				if (table.getValueAt(row, 1).toString().substring(0, 5).equals("Addre")) {
					editor = new DefaultCellEditor(oComboBox);
				} else {
					JTextField roTextField = new JTextField();
					roTextField.setEditable(false);
					editor = new DefaultCellEditor(roTextField);
				}
			} 
			if (table.getValueAt(row, 1) == "LocalAuthorityName" && column == 3) {
				JComboBox<String> oComboBox = new JComboBox<String>();
				for (fhrsAuthority auth: FHRSPlugin.fhrsAuthorities) {
					oComboBox.addItem(auth.name);
				}
				oComboBox.setSelectedItem(value.toString());
				editor = new DefaultCellEditor(oComboBox);
			}

			return editor.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
	}
	
	static class osmTagsTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		String[] columnNames = { "Merge", "FHRS Data", "Tag", "New value", "Old Value" };
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
			return (col == 0 || col == 2 || col == 3);
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