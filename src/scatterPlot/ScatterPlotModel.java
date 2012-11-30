package scatterPlot;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

public class ScatterPlotModel {
	private static Vector<ExpressionData> dataTable;
	public HashMap<String, Category> categories;
	private Vector<String> columnNames;
	private Vector<String> dataColumnNames;
	private Vector<Double> maxs;
	private Vector<Double> mins;
	private double max;
	private double min;
	public int maxCategotySize = 0;

	public Vector<String> getColumnNames() {
		return columnNames;
	}

	public Vector<String> getDataColumnNames() {
		return dataColumnNames;
	}

	public Vector<ExpressionData> getDataTable() {
		return dataTable;
	}

	public double getMax() {

		return max;
	}

	public double getMax(int index) {
		return maxs.get(index);
	}

	public double getMaxA() {
		return 10;
	}

	public double getMin() {

		return min;
	}

	public double getMin(int index) {
		return mins.get(index);
	}

	public void readData() {

	}

	public void readTXTData(String filename) {
		// initializing
		columnNames = new Vector<String>();
		dataTable = new Vector<ExpressionData>();
		categories = new HashMap<String, Category>();
		dataColumnNames = new Vector<String>();

		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String buffer;
			buffer = in.readLine();
			String[] tokens = buffer.split("\t");
			for (int i = 0; i < tokens.length; i++) {
				columnNames.add(tokens[i]);
			}
			for (int i = 1; i < tokens.length - 1; i++) {
				dataColumnNames.add(tokens[i]);
			}
			maxs = new Vector<Double>(columnNames.size() - 2);
			for (int i = 0; i < columnNames.size() - 2; i++) {
				maxs.add(Double.MIN_VALUE);
			}
			mins = new Vector<Double>(columnNames.size() - 2);
			for (int i = 0; i < columnNames.size() - 2; i++) {
				mins.add(Double.MAX_VALUE);
			}

			while ((buffer = in.readLine()) != null) {
				Vector<Object> expressionData = new Vector<Object>();
				tokens = buffer.split("\t");
				for (int i = 0; i < tokens.length - 1; i++) {
					expressionData.add(tokens[i]);
				}
				Category category = categories.get(tokens[tokens.length - 1]);
				if (category == null) {
					category = new Category();
					category.category = tokens[tokens.length - 1];
				}
				categories.put(tokens[tokens.length - 1], category);
				expressionData.add(category);

				for (int i = 0; i < tokens.length - 2; i++) {
					double d = Double.parseDouble(tokens[i + 1]);
					if (mins.get(i) > d) {
						mins.set(i, d);
					}
					if (maxs.get(i) < d) {
						maxs.set(i, d);
					}
				}

				ExpressionData newData = new ExpressionData(expressionData);
				category.addData(newData);

				dataTable.add(newData);
			}

			min = mins.get(0);
			for (int i = 1; i < mins.size(); i++) {
				if (min > mins.get(i)) {
					min = mins.get(i);
				}
			}
			max = maxs.get(0);
			for (int i = 1; i < maxs.size(); i++) {
				if (max < maxs.get(i)) {
					max = maxs.get(i);
				}
			}

			// max category size
			for (Category c : categories.values()) {
				if (c.data.size() > maxCategotySize) {
					maxCategotySize = c.data.size();
				}
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
