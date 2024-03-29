package scatterPlot;

import java.util.Vector;

public class ScatterPlot implements Runnable {
	static ScatterPlotModel model;
	static ScatterPlotView view;
	static ScatterPlotController controller;

	public ScatterPlot(Vector<Vector <Object>> data){
		model = new ScatterPlotModel();
		model.readTXTData("RPKM.txt");
		view = new ScatterPlotView(model);
		controller = new ScatterPlotController(view, model);
	}
	public ScatterPlot(String input){
		model = new ScatterPlotModel();
		model.readTXTData(input);
		view = new ScatterPlotView(model);
		controller = new ScatterPlotController(view, model);
	}
	public ScatterPlot(){
		model = new ScatterPlotModel();
		model.readTXTData("RPKM.txt");
		view = new ScatterPlotView(model);
		controller = new ScatterPlotController(view, model);
	}
	public static void main(String[] args){
		new ScatterPlot();
		view.start();
	}
	@Override
	public void run() {
		view.start();
	}

}
