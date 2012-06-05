package scatterPlot;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;


public class ScatterPlotModel {
	private static Vector<ExpressionData> dataTable = new Vector<ExpressionData>();
	private double minX = Double.MAX_VALUE;
	private double maxX = Double.MIN_VALUE;
	private double minY = Double.MAX_VALUE;
	private double maxY = Double.MIN_VALUE;
	private double maxA = Double.MIN_VALUE;
	public HashMap<String, Category> catetories = new HashMap<String, Category>();
	public HashMap<String, Category> biggerCategoties = new HashMap<String, Category>();

	public Vector<ExpressionData> getDataTable(){
		return dataTable;
	}
	public double getMin(){
		return Math.min(getMinX(), getMinY());
	}
	public double getMax(){
		return Math.max(getMaxX(), getMaxY());
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
	public double getMaxA(){
		return maxA;
	}
	public Vector<String> getColumnNames(){
		Vector<String> names = new Vector<String>();
		names.add("Feature ID");
		names.add("RPKM1");
		names.add("RPKM2");
		names.add("COG Category");
		return names;
	}

	public Vector<ExpressionData> readSQLData(String filename, String FeatureID, String RPKM1, String RPKM2, String Category){
		Vector<ExpressionData> table = new Vector<ExpressionData>();

		/*

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
		*/
		return table;

	}
	public void readTXTData(String filename, String FeatureID, String RPKM1, String RPKM2, String Category){
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String buffer;
			int indexID = -1;
			int indexRPKM1 = -1;
			int indexRPKM2 = -1;
			int indexCategory = -1;
			buffer = in.readLine();
			String[] tokens = buffer.split("\t");
			for(int i = 0; i < tokens.length; i++){
				if(tokens[i].compareTo(FeatureID)==0){
					indexID = i;
				}
				else if(tokens[i].compareTo(RPKM1)==0){
					indexRPKM1 = i;
				}
				else if(tokens[i].compareTo(RPKM2)==0){
					indexRPKM2 = i;
				}
				else if(tokens[i].compareTo(Category)==0){
					indexCategory = i;
				}
				else{
					throw new IOException("Unconsistency between given column names and those in data.");
				}
			}



			while((buffer = in.readLine()) != null){
				tokens = buffer.split("\t");
				String name = tokens[indexID];
				double x = Double.parseDouble(tokens[indexRPKM1]);
				double y = Double.parseDouble(tokens[indexRPKM2]);
				Category category = catetories.get(tokens[indexCategory]);
				if(category == null){
					category = new Category();
					category.category = tokens[indexCategory];
				}
				catetories.put(tokens[indexCategory], category);

				Category category2 = biggerCategoties.get(tokens[indexCategory].substring(0, 1));
				if(category2 == null){
					category2 = new Category();
					category2.category = tokens[indexCategory].substring(0, 1);
				}
				biggerCategoties.put(tokens[indexCategory].substring(0, 1), category2);



				if(x < minX && x != 0){
					minX = x;
				}
				else if(x > maxX){
					maxX = x;
				}
				if(y < minY && y != 0){
					minY = y;
				}
				else if(y > maxY){
					maxY = y;
				}
				if(x != 0  && y != 0 && Math.max(x, y) / Math.min(x, y) > maxA){
					maxA = Math.max(x, y) / Math.min(x, y);
				}

				ExpressionData newData = new ExpressionData(name, x, y, category);
				category2.addData(newData);
				category.addData(newData);
				dataTable.add(newData);
			}



		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void readData(){

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
