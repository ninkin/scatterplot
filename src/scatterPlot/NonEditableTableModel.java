package scatterPlot;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;

public class NonEditableTableModel extends DefaultTableModel {
	public NonEditableTableModel(Vector<ExpressionData> vector,
			Vector<String> vector2) {
		// TODO Auto-generated constructor stub
		super(vector, vector2);
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}
}
