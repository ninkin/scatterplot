package scatterPlot;
import java.util.Vector;

import javax.swing.JLabel;


public class ExpressionData extends Vector<Object>{
	public ExpressionData(String name, double x, double y, String category){
		this.add(name);
		this.add(x);
		this.add(y);
		this.add(category);
	}

	public String getName(){
		return (String)this.get(0);
	}
	public double getX(){
		return (Double) this.get(1);
	}
	public double getY(){
		return (Double)this.get(2);
	}
	public String getCategory(){
		return (String)this.get(3);
	}
}
