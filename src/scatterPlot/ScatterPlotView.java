package scatterPlot;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.renderer.lwjgl.LWJGLRenderer;
import de.matthiasmann.twl.theme.ThemeManager;
import delaunay_triangulation.Delaunay_Triangulation;
import delaunay_triangulation.Point_dt;
import delaunay_triangulation.Triangle_dt;
public class ScatterPlotView extends Widget {
	Vector<Color> colorPalette;
	// UI initialize
	private final static AtomicReference<Dimension> newCanvasSize = new AtomicReference<Dimension>();
	boolean closeRequested = false;

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
	Delaunay_Triangulation DT;


	//to picking
	IntBuffer selectBuff;
	int renderMode = GL11.GL_RENDER;
	int clickedIndex =-1;
	int overedIndex =-1;

	ThemeManager themeManager;

	//to print text
	Label toolTipBox = new Label();


	//layouts
	JFrame frame = new JFrame();
	final Canvas canvas = new Canvas();

	//axis
	Label xAxisLabel = new Label("X");
	Label yAxisLabel = new Label("Y");
	Label zAxisLabel = new Label("Z");

	//Buttons
	JButton xyButton = new JButton();
	JButton yzButton = new JButton();
	JButton xzButton = new JButton();


	public ScatterPlotView(List<ExpressionData> dataTable){
		this.dataTable = dataTable;
	}

	public static void main(String[] argv) {
		spModel = new ScatterPlotModel();
		spModel.readData();
		ScatterPlotView spView = new ScatterPlotView(spModel.getDataTable());

		spView.start();
	}

	public void start() {
		maxXY = Math.max(spModel.getMaxX(), spModel.getMaxY());
		camera = new Camera(-2*maxXY, 2*maxXY, -2*maxXY, 2*maxXY, -2*maxXY, 2*maxXY);
		initLayout();
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
		GUI gui = initTWL();
		initOpenGL();
		makeColorPalette();
		initTerrain();
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
		Display.destroy();
		frame.dispose();

		System.exit(0);
	}

	private GUI initTWL() {
		LWJGLRenderer renderer;
		try {
			renderer = new LWJGLRenderer();
			GUI gui = new GUI(this, renderer);
            themeManager = ThemeManager.createThemeManager(getClass().getResource("lesson1.xml"), renderer);
            gui.applyTheme(themeManager);
            toolTipBox.setAutoSize(true);
            toolTipBox.setTheme("label");
            xAxisLabel.setTheme("bigLabel");
            yAxisLabel.setTheme("bigLabel");
            zAxisLabel.setTheme("bigLabel");

    		add(toolTipBox);
    		add(xAxisLabel);
    		add(yAxisLabel);
    		add(zAxisLabel);
    		return gui;
		} catch (LWJGLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	private void initLayout() {
		xyButton.setText("XY");
		xyButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				super.mouseClicked(e);
				camera.setView("XY");
			}
		});

		yzButton.setText("YZ");
		yzButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				super.mouseClicked(e);
				camera.setView("YZ");
			}
		});


		xzButton.setText("XZ");
		xzButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				super.mouseClicked(e);
				camera.setView("XZ");
			}
		});



		frame.setLayout(new BorderLayout());


		canvas.setSize(new Dimension(1000,700));

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
	}
	private void initOpenGL() {
		selectBuff = BufferUtils.createIntBuffer(1024);
		GL11.glSelectBuffer(selectBuff);

		GL11.glClearColor(1, 1, 1, 1);

		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_NORMALIZE);
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);

		GL11.glEnable(GL11.GL_POINT_SMOOTH);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
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
			toolTipBox.setText(data.name+" ("+data.x+", "+data.y+")");
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
			GL11.glColor3d(0, 0, 0);
			GL11.glBegin(GL11.GL_LINES);

			GL11.glVertex3d(e.x, e.y, e.z);
			GL11.glVertex3d(0, e.y, e.z);

			GL11.glVertex3d(0, e.y, e.z);
			GL11.glVertex3d(0, 0, e.z);

			GL11.glVertex3d(0, e.y, e.z);
			GL11.glVertex3d(0, e.y, 0);

			GL11.glVertex3d(e.x, e.y, e.z);
			GL11.glVertex3d(e.x, e.y, 0);

			GL11.glVertex3d(e.x, e.y, 0);
			GL11.glVertex3d(0, e.y, 0);

			GL11.glVertex3d(e.x, e.y, 0);
			GL11.glVertex3d(e.x, 0, 0);

			GL11.glVertex3d(e.x, e.y, e.z);
			GL11.glVertex3d(e.x, 0, e.z);

			GL11.glVertex3d(e.x, 0, e.z);
			GL11.glVertex3d(e.x, 0, 0);

			GL11.glVertex3d(e.x, 0, e.z);
			GL11.glVertex3d(0, 0, e.z);
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


		drawDots();
		if(renderMode != GL11.GL_SELECT){
			drawShadows();
			drawAxis(maxXY);
			drawTerrain();
		}


	}


	private void drawDots() {
		GL11.glInitNames();
		GL11.glPointSize(7);

		for(int i = 0; i < dataTable.size(); i++){
			ExpressionData item = dataTable.get(i);
			GL11.glPushName(i);
			GL11.glBegin(GL11.GL_POINTS);

			if(overedIndex == i||clickedIndex == i){
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


		Vector<Float[]> intersect = new Vector<Float[]>();
		float origin[] = new float[3];
		Vector<float[]> axes = new Vector<float[]>();

		IntBuffer viewport = BufferUtils.createIntBuffer(16);
		GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

		FloatBuffer modelMatrix = BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelMatrix);

		FloatBuffer projMatrix = BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projMatrix);
		FloatBuffer win_pos = BufferUtils.createFloatBuffer(16);

		GLU.gluProject(0, 0, 0, modelMatrix, projMatrix, viewport, win_pos);

		origin[0] = win_pos.get(0);
		origin[1] = win_pos.get(1);

		GLU.gluProject(100, 0, 0, modelMatrix, projMatrix, viewport, win_pos);
		float t_x[] = {win_pos.get(0), win_pos.get(1)};;
		axes.add(t_x);

		GLU.gluProject(0, 100, 0, modelMatrix, projMatrix, viewport, win_pos);
		float t_y[] = {win_pos.get(0), win_pos.get(1)};;
		axes.add(t_y);

		GLU.gluProject(0, 0, 100, modelMatrix, projMatrix, viewport, win_pos);
		float t_z[] = {win_pos.get(0), win_pos.get(1)};;
		axes.add(t_z);



		for(int i = 0; i < 3; i++){
			float[] axisSample = axes.get(i);
			float intersectX0 = (axisSample[0] - origin[0])/(axisSample[1] - origin[1])*(0-origin[1])+origin[0]; // intersection point with bottom boundary
			Float[] a = {intersectX0, 0f};
			intersect.add(a);
			Float intersectX1 = (axisSample[0] - origin[0])/(axisSample[1] - origin[1])*(viewport.get(3)-origin[1])+origin[0];// intersection point with top boundary
			Float[] b= {intersectX1, (float)viewport.get(3)};
			intersect.add(b);
			Float intersectY0 = (axisSample[1] - origin[1])/(axisSample[0] - origin[0])*(0-origin[0])+origin[1];// intersection point with left boundary
			Float[] c= {0f, intersectY0};
			intersect.add(c);
			Float intersectY1 = (axisSample[1] - origin[1])/(axisSample[0] - origin[0])*(viewport.get(2)-origin[0])+origin[1];// intersection point with right boundary
			Float[] d= {(float)viewport.get(2), intersectY1};
			intersect.add(d);

			for(Float[] intersectPoint: intersect){

				if(intersectPoint[0] < 0 || intersectPoint[0] > viewport.get(2)
						|| intersectPoint[1] < 0 || intersectPoint[1] > viewport.get(3)
						|| (axisSample[0]-origin[0]) * (intersectPoint[0] - origin[0]) < 0
						|| (axisSample[1]-origin[1]) * (intersectPoint[1] - origin[1]) < 0 ){
				}
				else{

					if(intersectPoint[0]==0){
						intersectPoint[0] += 10;
					}
					else if(intersectPoint[0]==viewport.get(2)){
						intersectPoint[0] -= 10;
					}
					if(intersectPoint[1]==0){
						intersectPoint[1] += 10;
					}
					else if(intersectPoint[1]==viewport.get(3)){
						intersectPoint[1] -= 10;
					}
					int xPos = intersectPoint[0].intValue();
					int yPos = viewport.get(3) - intersectPoint[1].intValue();
					//check is in viewport
					Label axisLabel = xAxisLabel;
					switch(i){
						case 0:
							axisLabel = xAxisLabel;
							GLU.gluProject((float) maxXY, 0, 0, modelMatrix, projMatrix, viewport, win_pos);
							break;
						case 1:
							axisLabel = yAxisLabel;
							GLU.gluProject(0, (float) maxXY, 0, modelMatrix, projMatrix, viewport, win_pos);
							break;
						case 2:
							axisLabel = zAxisLabel;
							GLU.gluProject(0, 0, (float) maxXY, modelMatrix, projMatrix, viewport, win_pos);
							break;
					}
					if(win_pos.get(0) > 0 && win_pos.get(0) < viewport.get(2) && win_pos.get(1) > 0 && win_pos.get(1) < viewport.get(3)){
						xPos = (int) win_pos.get(0);
						yPos = viewport.get(3) - (int) win_pos.get(1);
					}
					axisLabel.setPosition(xPos, yPos);


				}
			}
		}
	}
	private void initTerrain(){
		Point_dt[] ps = new Point_dt[dataTable.size()];
		for(int i = 0; i < dataTable.size(); i++){
			ps[i] = new Point_dt(dataTable.get(i).x, dataTable.get(i).y, dataTable.get(i).z);
		}
		DT = new Delaunay_Triangulation(ps);
	}
	private void drawTerrain(){

		Iterator<Triangle_dt> iter = DT.trianglesIterator();
		GL11.glColor3d(0.8, 0.8, 0.8);
		while(iter.hasNext()){

			GL11.glBegin(GL11.GL_LINE_LOOP);
			Triangle_dt tri = iter.next();
			GL11.glVertex3d(tri.p1().x(), tri.p1().y(), tri.p1().z());
			GL11.glVertex3d(tri.p2().x(), tri.p2().y(), tri.p2().z());
			if(tri.p3() == null){
				GL11.glVertex3d(tri.p2().x(), tri.p2().y(), tri.p2().z());
			}
			else
			{
				GL11.glVertex3d(tri.p3().x(), tri.p3().y(), tri.p3().z());
			}
			GL11.glEnd();
		}

	}
}
