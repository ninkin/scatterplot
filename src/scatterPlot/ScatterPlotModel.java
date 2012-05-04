package scatterPlot;
import java.beans.Expression;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;


public class ScatterPlotModel {
	private static Vector<ExpressionData> dataTable = new Vector<ExpressionData>();
	public static Vector<Object> detail = new Vector<Object>();
	private double minX;
	private double maxX;
	private double minY;
	private double maxY;
	private static int numX;
	private static int numY;

	public int getNumX(){
		return numX;
	}
	public int getNumY(){
		return numY;
	}
	public Vector<ExpressionData> getDataTable(){
		return dataTable;
	}
	public double getMinX(){
		return minX;
	}
	public double getMaxX(){
		return maxX;
	}
	public double getMinY(){
		return minY;
	}
	public double getMaxY(){
		return maxY;
	}
	public Vector<String> getColumnNames(){
		Vector<String> names = new Vector<String>();
		names.add("Feature ID");
		names.add("RPKM1");
		names.add("RPKM2");
		names.add("dens");
		names.add("X-Y");
		names.add("Expression Values");
		names.add("Gene Length");
		names.add("Unique Gene Reads");
		return names;
	}

	private Vector<ExpressionData> readSQLData(String filename, String FeatureID, String RPKM1, String RPKM2, String Category){
		Vector<ExpressionData> table = new Vector<ExpressionData>();
		return table;

	}
	private Vector<ExpressionData> readTXTData(String filename, String FeatureID, String RPKM1, String RPKM2, String Category){
		Vector<ExpressionData> table = new Vector<ExpressionData>();
		return table;

	}
	@SuppressWarnings("unchecked")
	public void readData(){
		maxX = Double.MIN_VALUE;
		minX = Double.MAX_VALUE;
		maxY = Double.MIN_VALUE;
		minY = Double.MAX_VALUE;
		try {
			Class.forName("org.sqlite.JDBC");
			Connection conn = DriverManager.getConnection("jdbc:sqlite:Sample01.clt");
			Statement stat = conn.createStatement();
			stat.execute("ATTACH DATABASE \"Sample02.clt\" AS Sample02");
			ResultSet rs = stat.executeQuery("SELECT a.Feature_ID, a.RPKM as RPKM1, b.RPKM as RPKM2, a.Expression_Values, a.Gene_length," +
					" a.Unique_gene_reads, a.Total_gene_reads, a.Chromosome, a.Chromosome_region_start," +
					" a.Chromosome_region_end FROM main.RPKM a INNER JOIN Sample02.RPKM b ON a.RowID = b.RowID");
			do{



				String name = rs.getString(1);
				double x = rs.getDouble(2);
				double y = rs.getDouble(3);
				ExpressionData newData = new ExpressionData(name, x, y, 0);
				newData.addData(rs.getDouble(4));
				newData.addData(rs.getDouble(5));
				newData.addData(rs.getDouble(6));
				newData.addData(rs.getDouble(7));

				dataTable.add(newData);


				Vector<Object> data = new Vector<Object>();
				data.add(rs.getString(1));
				data.add(rs.getDouble(2));
				detail.add(data);



				if(maxX < x){
					maxX = x;
				}
				else if(minX > x){
					minX = x;
				}
				if(maxY < y){
					maxY = y;
				}
				else if(minY > y){
					minY = y;
				}
			} while(rs.next());

		}
		catch(SQLException e){
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private double kernelFunction(ExpressionData data0, ExpressionData data1){
		int interval = 3;
		double x0 = data0.getX();
		double x1 = data1.getX();
		double y0 = data0.getY();
		double y1 = data1.getY();
		double distance = Math.sqrt(Math.pow((x0-x1), 2) + Math.pow((y0-y1), 2));
		if(distance > interval * 3){
			return 0;
		}
		else if(distance > interval * 2){
			return 1;
		}
		else if(distance > interval){
			return 2;
		}
		else{
			return 3;
		}

	}
}
