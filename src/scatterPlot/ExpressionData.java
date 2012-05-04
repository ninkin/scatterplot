package scatterPlot;
import java.util.Vector;

import javax.swing.JLabel;


public class ExpressionData extends Vector<Object>{
	public ExpressionData(String name, double x, double y, double z){
		this.add(name);
		this.add(x);
		this.add(y);
		this.add(z);
		this.add(Math.abs(Math.log10(x)-Math.log10(y)));
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
	public double getZ(){
		return (Double)this.get(3);
	}
	public void addData(Object e){
		this.add(e);
	}
	public double getDiff(){
		return (Double) this.get(4);
	}
	public void setZ(double z){
		this.setElementAt(z, 3);
	}
}
