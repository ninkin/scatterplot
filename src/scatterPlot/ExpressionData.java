package scatterPlot;
import java.util.Vector;

import javax.swing.JLabel;


public class ExpressionData extends Vector<Object>{
	public boolean isDimming = false;
	final double DEFAULT_ALPHA = 0.8;
	public double alpha = DEFAULT_ALPHA;
	public ExpressionData(Vector<Object> data){
		this.addAll(data);
	}

	public String getName(){
		return (String)this.get(0);
	}
	public Category getCategory(){
		return (Category)this.get(this.size()-1);
	}
	public double getExp(int i){
		return Double.parseDouble(""+this.get(i+1));
	}
}
