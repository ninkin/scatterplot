package scatterPlot;
import java.util.Vector;


public class ExpressionData implements Comparable{
	String name;
	double x;
	double y;
	double z;
	Vector<Object> data;


	public ExpressionData(String name, double x, double y, double z){
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		data = new Vector<Object>();
	}
	public String getName(){
		return name;
	}
	public double getX(){
		return x;
	}
	public double getY(){
		return y;
	}
	public double getZ(){
		return z;
	}
	public void addData(Object e){
		data.add(e);
	}
	public void setZ(double z){
		this.z = z;
	}
	public Vector<Object> getData(){
		return data;
	}
	@Override
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		return z > ((ExpressionData)arg0).z ? 1 : -1;
	}

}
