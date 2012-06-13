package scatterPlot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

public class ScatterPlotController {
	private ScatterPlotView view;
	private ScatterPlotModel model;
	public ScatterPlotController(ScatterPlotView view, ScatterPlotModel model){
		this.view = view;
		this.model = model;
	}
	public void addView(){

	}
	class FileOpenListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
		}
	}

}