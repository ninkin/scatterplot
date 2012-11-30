package scatterPlot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ScatterPlotController {
	class FileOpenListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
		}
	}
	private ScatterPlotView view;

	private ScatterPlotModel model;

	public ScatterPlotController(ScatterPlotView view, ScatterPlotModel model) {
		this.view = view;
		this.model = model;
	}

	public void addView() {

	}

}