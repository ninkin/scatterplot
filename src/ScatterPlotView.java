import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;
import javax.swing.JSlider;
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
import org.lwjgl.util.glu.GLU;


public class ScatterPlotView {

	private final static AtomicReference<Dimension> newCanvasSize = new AtomicReference<Dimension>();
	float[][] visualTable;
	Vector<ExpressionData> dataTable;
	static ScatterPlotModel spModel;
	int numX;
	int numY;
	private Camera camera = new Camera(0, 0, 0);
	boolean closeRequested = false;


	public ScatterPlotView(Vector<ExpressionData> dataTable, float[][] visualTable, int numX, int numY){
		this.dataTable = dataTable;
		this.visualTable = visualTable;
		this.numX = numX;
		this.numY = numY;
	}

	public static void main(String[] argv) {
		spModel = new ScatterPlotModel();
		spModel.readData();
		ScatterPlotView spView = new ScatterPlotView(spModel.getDataTable(), spModel.getVisualTable(), spModel.getNumX(), spModel.getNumY());
		spView.start();
	}

	public void start() {
		JFrame frame = new JFrame();
		frame.setLayout(new BorderLayout());
		final Canvas canvas = new Canvas();
		JSlider sliderX = new JSlider(0, 90);
		sliderX.setValue(0);
		sliderX.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				camera.setYaw(((JSlider)e.getSource()).getValue());
			}

		});
		JSlider sliderY = new JSlider(JSlider.VERTICAL, -90, 0, 0);
		sliderY.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				camera.setPitch(((JSlider)e.getSource()).getValue());
			}

		});


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

		frame.add(canvas, BorderLayout.CENTER);
		frame.add(sliderX, BorderLayout.SOUTH);
		frame.add(sliderY, BorderLayout.WEST);

		try {
			canvas.setVisible(true);
			frame.setVisible(true);
			Display.setParent(canvas);
			Display.setVSyncEnabled(true);

			frame.setPreferredSize(new Dimension(1024, 768));
			frame.setMinimumSize(new Dimension(800, 600));
			frame.pack();
			Display.create();

		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(0);
		}

		init();
		while (!Display.isCloseRequested() && ! closeRequested){
			if(Mouse.isButtonDown(0)){

			}




			GL11.glLoadIdentity();
			camera.lookThrough();

			//GLU.gluLookAt(0, 0, 0, 0, 1, -1, 0, 1, 1);

			display();
			mouseClickHandler(Mouse.getX(), Mouse.getY());
			Display.update();
		}
		//Display.destroy();
		frame.dispose();
		System.exit(0);
	}



	private void init(){
		FloatBuffer mat_specular = BufferUtils.createFloatBuffer(4);
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
		wlmodel_ambient.rewind();
		GL11.glClearColor(0, 0, 0, 0);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, mat_specular);
		GL11.glMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT, mat_specular);
		GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, mat_specular);
		GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, mat_shininess);
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, light_position);
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, white_light);
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_AMBIENT, wlmodel_ambient);

		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_LIGHT0);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_NORMALIZE);
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();

		GL11.glOrtho(0, numX, 0, numY, -100, 100);
		//GL11.glOrtho(0, 800, 0, 600, 1, -1);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
	}
	void mouseClickHandler(int x, int y){
		IntBuffer selectBuff = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder()).asIntBuffer();
		int hits;
		IntBuffer viewport = BufferUtils.createIntBuffer(16);

		GL11.glSelectBuffer(selectBuff);
		GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GL11.glRenderMode(GL11.GL_SELECT);
		GL11.glLoadIdentity();
		GLU.gluPickMatrix(x, y, 2, 2, viewport);
		GL11.glOrtho(0, numX, 0, numY, -100, 100);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		camera.lookThrough();
		display();
		hits = GL11.glRenderMode(GL11.GL_RENDER);
		if(hits > 0){

			System.out.print(hits);
			System.out.println();
		}
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);

	}



	private void display(){
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);


		GL11.glColorMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT);


		/*
		for(int i = 0 ; i < x-1; i++){
			GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

			for(int j = 0 ; j < y; j++){
				GL11.glColor3d(visualTable[i][j], 1, 1);
				//GL11.glNormal3d(visualItemTable[i][j]-visualItemTable[i+1][j], visualItemTable[i][j+1]-visualItemTable[i][j], 1);
				GL11.glVertex3d(i, j, visualTable[i][j]);
				GL11.glColor3d(visualTable[i+1][j], 1, 1);
				//GL11.glNormal3d(visualItemTable[i+1][j]-visualItemTable[i][j], visualItemTable[i][j]-visualItemTable[i][j+1], -1);
				GL11.glVertex3d(i+1, j, visualTable[i+1][j]);

			}
			GL11.glEnd();
		}
		*/
		GL11.glInitNames();
		GL11.glPointSize(3);
		GL11.glBegin(GL11.GL_POINTS);
		double lod = spModel.getLod();
		for(int i = 0; i < dataTable.size(); i++){
			ExpressionData item = dataTable.get(i);
			int x = (int)(item.getX()/lod);
			int y = (int)(item.getY()/lod);
			GL11.glColor3d(0, 1, 0);


			double z =
					visualTable[x][y] * (x+1-item.x/lod)*(y+1-item.y/lod)
					+ visualTable[x][(y+1)%numY] * (x+1-item.x/lod)*(y-item.y/lod)
					+ visualTable[(x+1)%numX][y] * (x-item.x/lod)*(y+1-item.y/lod)
					+ visualTable[(x+1)%numX][(y+1)%numY] * (x-item.x/lod)*(y-item.y/lod);
			GL11.glPushName(i);
			GL11.glPushMatrix();
			GL11.glVertex3d(item.x/lod, item.y/lod, z);
			GL11.glPopMatrix();
			GL11.glPopName();


		}
		GL11.glEnd();
	}

}
