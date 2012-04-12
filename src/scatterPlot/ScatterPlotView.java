package scatterPlot;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.basic.BasicSliderUI;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.GLUtessellator;

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.TextArea;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.renderer.lwjgl.LWJGLRenderer;
import de.matthiasmann.twl.theme.ThemeManager;
import static org.lwjgl.util.glu.GLU.*;

public class ScatterPlotView extends Widget {


	Vector<Color> colorPalette;
	// UI initialize
	private final static AtomicReference<Dimension> newCanvasSize = new AtomicReference<Dimension>();
	boolean closeRequested = false;

	JLabel nameLabel = new JLabel();
	//camera
	private static Camera camera;

	//database
	List<ExpressionData> dataTable;
	static ScatterPlotModel spModel;
	JTable detailTable;

	//for landscape
	double [][]landscape;
	double maxXY;
	int lod = 100;

	//to picking
	IntBuffer selectBuff;
	int renderMode = GL11.GL_RENDER;
	int clickedIndex =-1;
	int overedIndex =-1;

	//to tesselate
	int tessList;
	private GLUtessellator tesselator;
	ThemeManager themeManager;

	//to print text
	Button toolTipBox = new Button();


	public ScatterPlotView(List<ExpressionData> dataTable){
		this.dataTable = dataTable;
	}

	public static void main(String[] argv) {
		spModel = new ScatterPlotModel();
		spModel.readData();
		ScatterPlotView spView = new ScatterPlotView(spModel.getDataTable());

		spView.start();
	}
	private void makeLandscape(){
		Vector<Vector<Vector<Double>>> tempZsumVector =  new  Vector<Vector<Vector< Double >>> (); ;

		landscape = new double [(int) (maxXY/lod)+1][(int) (maxXY/lod)+1];
		for(int i = 0; i < maxXY/lod; i++){
			tempZsumVector.add(new Vector<Vector<Double>>());
			for(int j = 0; j < maxXY/lod; j++){
				tempZsumVector.get(i).add(new Vector<Double>());
			}
		}
		for(ExpressionData e : dataTable){
			tempZsumVector.get((int)e.x/lod).get((int)e.y/lod).add(e.z);
		}
		for(int i = 0; i < tempZsumVector.size(); i++){
			for(int j = 0; j < tempZsumVector.get(i).size(); j++){
				double zSum=0;
				for(Double g : tempZsumVector.get(i).get(j)){
					zSum += g.doubleValue();
				}
				landscape[i][j] = zSum/tempZsumVector.get(i).get(j).size();
			}
		}
	}

	public void start() {
		maxXY = Math.max(spModel.getMaxX(), spModel.getMaxY());
		camera = new Camera(-2*maxXY, 2*maxXY, -2*maxXY, 2*maxXY, -2*maxXY, 2*maxXY);

/*		Vector<Vector<Object>> tempTable = new Vector<Vector<Object>>();
		for(ExpressionData e : dataTable){
			Vector<Object> newData = new Vector<Object>();
			newData.add(e.x);
			newData.add(e.y);
			newData.add(e.z);
			for(Object data : e.getData()){
				newData.add(data);
			}
		}
		Vector<String> columnName = new Vector<String>();
		columnName.add("x");
		columnName.add("y");
		columnName.add("z");
		columnName.add("Expression Value");
		columnName.add("Gene Length");
		columnName.add("Unique_gene_reads");
		columnName.add("Chromosome");
		columnName.add("Chromosome_region_start");
		columnName.add("Chromosome_region_end");*/


		//detailTable = new JTable(tempTable, columnName );
		//detailTable.setPreferredSize(new Dimension(200,500));

		JButton xyButton = new JButton();
		xyButton.setText("XY");
		xyButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				super.mouseClicked(e);
				camera.setView("XY");
			}
		});

		JButton yzButton = new JButton();
		yzButton.setText("YZ");
		yzButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				super.mouseClicked(e);
				camera.setView("YZ");
			}
		});

		JButton xzButton = new JButton();
		xzButton.setText("XZ");
		xzButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				super.mouseClicked(e);
				camera.setView("XZ");
			}
		});


		JFrame frame = new JFrame();
		frame.setLayout(new BorderLayout());


		final Canvas canvas = new Canvas();
		canvas.setPreferredSize(new Dimension(1000,700));

		nameLabel.setMinimumSize(new Dimension(1000, 20));







		canvas.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e)
			{ newCanvasSize.set(canvas.getSize()); }
		});

		frame.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent e)
			{ canvas.requestFocusInWindow(); }
		});
		frame.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e){
				closeRequested = true;
			}
		});

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.add(canvas);
		centerPanel.add(nameLabel);
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.add(xyButton);
		rightPanel.add(yzButton);
		rightPanel.add(xzButton);


		frame.add(centerPanel, BorderLayout.CENTER);
		frame.add(leftPanel, BorderLayout.WEST);
		frame.add(rightPanel, BorderLayout.EAST);

		try {

			canvas.setVisible(true);
			frame.setVisible(true);
			Display.setParent(canvas);

			Display.setVSyncEnabled(true);


			frame.setPreferredSize(new Dimension(1024, 800));
			frame.setMinimumSize(new Dimension(800, 600));
			frame.pack();
			Display.create();


		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(0);
		}
		LWJGLRenderer renderer = null;

		try {
			renderer = new LWJGLRenderer();
		} catch (LWJGLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		GUI gui = new GUI(this, renderer);
		 try{
             themeManager = ThemeManager.createThemeManager(getClass().getResource("lesson1.xml"), renderer);
        } catch(IOException e){
            e.printStackTrace();
        }

        gui.applyTheme(themeManager);
		add(toolTipBox);
        toolTipBox.setSize(100, 30);
		init();
		makeLandscape();
		while (!Display.isCloseRequested() && ! closeRequested){

			display();
			mouseClickHandler(Mouse.getX(), Mouse.getY());
			if(toolTipBox.getText()==null)
				toolTipBox.setVisible(false);
			else
				toolTipBox.setVisible(true);
			toolTipBox.setPosition(Mouse.getX()+20, canvas.getHeight() - Mouse.getY()+10);
			gui.update();
			Display.update();
		}
		//Display.destroy();

		tesselator.gluDeleteTess();
		frame.dispose();
		System.exit(0);
	}



	private void init(){
		selectBuff = BufferUtils.createIntBuffer(1024);
		GL11.glSelectBuffer(selectBuff);
/*		FloatBuffer mat_specular = BufferUtils.createFloatBuffer(4);
		mat_specular.put(1);
		mat_specular.put(0);
		mat_specular.put(0);
		mat_specular.put(1);
		mat_specular.rewind();

		FloatBuffer light_position = BufferUtils.createFloatBuffer(4);
		light_position.put(1);
		light_position.put(1);
		light_position.put(1);
		light_position.put(1);
		light_position.rewind();

		float mat_shininess = 70;

		FloatBuffer white_light = BufferUtils.createFloatBuffer(4);
		white_light.put(1);
		white_light.put(1);
		white_light.put(1);
		white_light.put(1);
		white_light.rewind();

		FloatBuffer wlmodel_ambient = BufferUtils.createFloatBuffer(4);
		wlmodel_ambient.put(0.1f);
		wlmodel_ambient.put(0.1f);
		wlmodel_ambient.put(0.1f);
		wlmodel_ambient.put(0.1f);
		wlmodel_ambient.rewind();*/
		GL11.glClearColor(1, 1, 1, 1);
/*		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, mat_specular);
		GL11.glMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT, mat_specular);
		GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, mat_specular);
		GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, mat_shininess);
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, light_position);
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, white_light);
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_AMBIENT, wlmodel_ambient);*/

/*		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_LIGHT0);*/
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_NORMALIZE);
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);

		GL11.glEnable(GL11.GL_POINT_SMOOTH);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		makeColorPalette();
		//tessellate();
	}
	private void makeColorPalette() {
		colorPalette = new Vector<Color>();
		colorPalette.add(new Color(190, 160, 37));
		colorPalette.add(new Color(190, 37, 37));
	}

	void mouseClickHandler(int x, int y){
		GL11.glRenderMode(GL11.GL_SELECT);
		renderMode = GL11.GL_SELECT;
		display();
		renderMode = GL11.GL_RENDER;
		int hits = GL11.glRenderMode(GL11.GL_RENDER);
		if(hits > 0){
			int depth, choose;
			choose = selectBuff.get(3);
			depth = selectBuff.get(1);
			for(int loop = 1; loop < hits ; loop++){
				if(selectBuff.get(loop*4+1) < depth){
					choose = selectBuff.get(loop*4+3);
					depth = selectBuff.get(loop*4+1);
				}
			}
			ExpressionData data = dataTable.get(choose);
			if(Mouse.isButtonDown(0)){
				clickedIndex = overedIndex;
			}
			else{
				overedIndex = choose;
			}
			nameLabel.setAlignmentX(0);
			DecimalFormat format  = new DecimalFormat(".##");
			toolTipBox.setText(data.name);
		}
		else{
			overedIndex = -1;
			toolTipBox.setText(null);
			if(Mouse.isButtonDown(0)&&clickedIndex != -1){
				clickedIndex = -1;
			}
		}
	}



	private void drawShadows() {
		if(clickedIndex != -1){
			ExpressionData e = dataTable.get(clickedIndex);
			GL11.glColor3d(0.8, 0.8, 0.8);
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex3d(e.x, e.y, e.z);
			GL11.glVertex3d(0, e.y, e.z);
			GL11.glVertex3d(e.x, e.y, e.z);
			GL11.glVertex3d(e.x, e.y, 0);
			GL11.glVertex3d(e.x, e.y, e.z);
			GL11.glVertex3d(e.x, 0, e.z);
			GL11.glEnd();

		}
	}

	private void display(){
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();

		if(renderMode == GL11.GL_SELECT){
			IntBuffer viewport = BufferUtils.createIntBuffer(16);
			GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
			GLU.gluPickMatrix(Mouse.getX(), Mouse.getY(), 3, 3, viewport);
		}
		double maxXY = Math.max(spModel.getMaxX(), spModel.getMaxY());
		camera.getInput();
		camera.move();

		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);


		showName();
		drawShadows();
		drawDots();
		drawAxis(maxXY);


	}
	private void showName(){
		if(overedIndex!=-1){
			ExpressionData data = dataTable.get(overedIndex);
			nameLabel.setText(data.name + " ("+ data.getX() + "," + data.getY() + ")");
		}
		else{
			nameLabel.setText(" ");
		}
	}
	private void drawLandscape(){
		GL11.glBegin(GL11.GL_POINTS);
		GL11.glColor3f(0, 0, 0);
		for(int i = 0; i < landscape.length-1; i++){
			for(int j = 0 ; j< landscape[i].length-1; j++){

				GL11.glVertex3d(i*lod, j*lod, landscape[i][j]);
				GL11.glVertex3d(i*lod, (j+1)*lod, landscape[i][j+1]);
			}
		}
		GL11.glEnd();
	}

	private void drawDots() {
		GL11.glInitNames();
		GL11.glPointSize(7);

		for(int i = 0; i < dataTable.size(); i++){
			ExpressionData item = dataTable.get(i);
			GL11.glPushName(i);
			GL11.glBegin(GL11.GL_POINTS);

			if(overedIndex == i){
				GL11.glColor3f(0, 1, 1);
			}
			else{
				GL11.glColor4d(
						(colorPalette.get(0).getRed()*(1-(double)i/dataTable.size())+colorPalette.get(1).getRed()*((double)i/dataTable.size()))/255.0,
						(colorPalette.get(0).getGreen()*(1-(double)i/dataTable.size())+colorPalette.get(1).getGreen()*((double)i/dataTable.size()))/255.0,
						(colorPalette.get(0).getBlue()*(1-(double)i/dataTable.size())+colorPalette.get(1).getBlue()*((double)i/dataTable.size()))/255.0,
						0.5);

			}

			GL11.glVertex3d(item.x, item.y, item.z);
			GL11.glEnd();
			GL11.glPopName();
		}

	}

	private void drawAxis(double maxXY) {
		int stride = (int)Math.pow(10, Math.floor(Math.log10(maxXY))-1);
		GL11.glLineWidth(1);
		GL11.glBegin(GL11.GL_LINES);

		for(int i = 0; i < maxXY; i+= stride ){
			if(i ==0 )
				GL11.glColor3f(0, 0, 0);
			else
				GL11.glColor3d(0.8, 0.8, 0.8);
			GL11.glVertex3d(0, i, 0);
			GL11.glVertex3d(maxXY, i, 0);
			GL11.glVertex3d(i, 0, 0);
			GL11.glVertex3d(i, maxXY, 0);
			GL11.glVertex3d(0, i, 0);
			GL11.glVertex3d(0, i, maxXY);
			GL11.glVertex3d(0, 0, i);
			GL11.glVertex3d(0, maxXY, i);
			GL11.glVertex3d(0, 0, i);
			GL11.glVertex3d(maxXY, 0, i);
			GL11.glVertex3d(i, 0, 0);
			GL11.glVertex3d(i, 0, maxXY);
		}
		GL11.glEnd();
	}
	private void tessellate(){

		tessList = GL11.glGenLists(2);
		tesselator = gluNewTess();
		TessCallback callback = new TessCallback();
		tesselator.gluTessCallback(GLU_TESS_VERTEX, callback);
		tesselator.gluTessCallback(GLU_TESS_BEGIN, callback);
		tesselator.gluTessCallback(GLU_TESS_END, callback);
		tesselator.gluTessCallback(GLU_TESS_COMBINE, callback);

		tesselator.gluTessProperty(GLU_TESS_WINDING_RULE, GLU_TESS_WINDING_NONZERO);
		tesselator.gluTessProperty(GLU_TESS_BOUNDARY_ONLY,GL11.GL_TRUE);
		tesselator.gluTessNormal(0, 0, 1);

		GL11.glNewList(tessList, GL11.GL_COMPILE);
			tesselator.gluTessBeginPolygon(null);
			tesselator.gluTessBeginContour();
			int i=0;
			for(ExpressionData data : dataTable){
				double[] coord = new double[6];
				coord[0] = data.x;
				coord[1] = data.y;
				coord[2] = data.z;
				tesselator.gluTessVertex(coord, 0, new VertexData(coord));
			}
			tesselator.gluTessEndContour();
			tesselator.gluTessEndPolygon();
		GL11.glEndList();

	}
}
