package scatterPlot;

import java.awt.Color;
import java.util.Vector;

public class Category {
	public boolean isActivated = true;
	public Color color;
	public String category;
	public Vector<ExpressionData> data = new Vector<ExpressionData>();

	public void addData(ExpressionData data) {
		this.data.add(data);
	}

	public void toggleActivation() {
		isActivated = !isActivated;
	}

	@Override
	public String toString() {
		return category;
	}

}
