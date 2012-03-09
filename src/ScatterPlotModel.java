import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;


public class ScatterPlotModel {
	static double lod = 1000;
	private static Vector<ExpressionData> dataTable = new Vector<ExpressionData>();
	private static float visualItemTable[][];
	private static int numX;
	private static int numY;
	private static float countMax;
	public float[][] getVisualTable(){
		return visualItemTable;
	}
	public int getNumX(){
		return numX;
	}
	public int getNumY(){
		return numY;
	}
	public Vector<ExpressionData> getDataTable(){
		return dataTable;
	}
	public double getLod(){
		return lod;
	}

	public void readData(){
		double maxX = Double.MIN_VALUE;
		double minX = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		try {
			Class.forName("org.sqlite.JDBC");
			Connection conn = DriverManager.getConnection("jdbc:sqlite:Sample01.clt");
			Statement stat = conn.createStatement();
			stat.execute("ATTACH DATABASE \"Sample02.clt\" AS Sample02");
			ResultSet rs = stat.executeQuery("SELECT a.Feature_ID, a.RPKM as RPKM1, b.RPKM as RPKM2, a.Expression_Values, a.Gene_length," +
					" a.Unique_gene_reads, a.Total_gene_reads, a.Chromosome, a.Chromosome_region_start," +
					" a.Chromosome_region_end FROM main.RPKM a INNER JOIN Sample02.RPKM b ON a.ROWID = b.ROWID");
			do{
				String name = rs.getString(1);
				double x = rs.getDouble(2);
				double y = rs.getDouble(3);
				dataTable.add(new ExpressionData(name, x, y, 0));
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

			System.out.println("maxX:"+maxX);
			System.out.println("minX:"+minX);
			System.out.println("maxY:"+maxY);
			System.out.println("minY:"+minY);
			numX = (int)(Math.ceil((maxX-minX)/lod));
			numY = (int)(Math.ceil((maxY-minY)/lod));
			visualItemTable = new float[numX][numY];

			for(ExpressionData data : dataTable){
				int x = (int)((data.getX()- minX)/lod);
				int y = (int)((data.getY()-minY)/lod);
				visualItemTable[x][y]++;
				if(visualItemTable[x][y] > countMax){
					countMax = visualItemTable[x][y];
				}
			}

			for(int i = 0; i < numX; i++){
				for(int j = 0; j < numY; j++){
					//visualItemTable[i][j]/=countMax;
				}
			}




		}
		catch(SQLException e){
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
