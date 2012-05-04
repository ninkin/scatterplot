package scatterPlot;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowFilter.ComparisonType;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

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
public class ScatterPlotView extends Widget {
	Vector<Color> colorPalette;
	// UI initialize
	private final static AtomicReference<Dimension> newCanvasSize = new AtomicReference<Dimension>();
	boolean closeRequested = false;

	//camera
	private static Camera camera;

	//database
	static ScatterPlotModel spModel;
	Vector<ExpressionData> dataVector;
	JTable detailTable;
	int totalSampleSize = 0;
	int numOfshowingDots = 0;

	double maxXY;


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

	//filtering values
	double equalFilter = 0;
	double smallFilter = -1000;
	List<RowFilter<Object, Object>> tableFilter = new ArrayList<RowFilter<Object, Object>>(4);

	//is log scale
	boolean isLogScale = true;

	JLabel statusLabel;

	public static void main(String[] argv) {
		spModel = new ScatterPlotModel();
		spModel.readData();
		ScatterPlotView spView = new ScatterPlotView();

		spView.start();
	}
	void initFilters(){
		tableFilter.add(RowFilter.regexFilter(""));
		tableFilter.add(RowFilter.regexFilter(""));
		tableFilter.add(RowFilter.regexFilter(""));
		tableFilter.add(RowFilter.regexFilter(""));
	}

	public void start() {
		if(isLogScale){
			maxXY = Math.log10(Math.max(spModel.getMaxX(), spModel.getMaxY()));
		}
		else{
			maxXY = Math.max(spModel.getMaxX(), spModel.getMaxY());
		}
		camera = new Camera(-maxXY/10, 1.1*maxXY, -maxXY/10, 1.1*maxXY, 1, -1);
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
		initFilters();
		while (!Display.isCloseRequested() && ! closeRequested){

			statusLabel.setText(detailTable.getRowCount()+"/"+totalSampleSize);
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

    		add(toolTipBox);
    		add(xAxisLabel);
    		add(yAxisLabel);
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
		JPanel centerPanel = new JPanel();
		JPanel statusPanel = new JPanel();
		JPanel rightPanel = new JPanel();




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

		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.add(canvas);
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));




		statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		statusPanel.setPreferredSize(new Dimension(frame.getWidth(), 16));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));

		statusLabel = new JLabel();

		statusPanel.add(statusLabel);

		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
/*		JCheckBox toggleLogScale = new JCheckBox("Log Scale");
		toggleLogScale.setSelected(true);
		toggleLogScale.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// TODO Auto-generated method stub
				isLogScale = ((JCheckBox)e.getSource()).isSelected();
				if(isLogScale){
					smallSlider.setMaximum((int)Math.log10(Math.max(spModel.getMaxX(), spModel.getMaxY())));
					smallSlider.setValue(0);
					equalSlider.setMaximum((int)Math.log10(Math.max(spModel.getMaxX(), spModel.getMaxY())));
					equalSlider.setValue(0);

				}
				else{
					smallSlider.setMaximum((int)(Math.max(spModel.getMaxX(), spModel.getMaxY())));
					smallSlider.setValue(0);
					equalSlider.setMaximum((int)(Math.max(spModel.getMaxX(), spModel.getMaxY())));
					equalSlider.setValue(0);
				}
			}
		});

		rightPanel.add(toggleLogScale);*/
		detailTable = new JTable(spModel.getDataTable(), spModel.getColumnNames());
		totalSampleSize = detailTable.getRowCount();
		detailTable.setRowSorter(new TableRowSorter<TableModel>(detailTable.getModel()));
		JScrollPane tablePane = new JScrollPane(detailTable);
		tablePane.setPreferredSize(new Dimension(400, 800));
		rightPanel.add(tablePane);


		JPanel smallSliderPanel = new JPanel();
		JLabel smallFilterLabel = new JLabel("RPKM Filter");
		final JTextField smallTextField = new JTextField(3);
		final JSlider smallSlider = new JSlider(-2000,
				(int)((isLogScale?Math.log10(Math.max(spModel.getMaxX(), spModel.getMaxY())): Math.max(spModel.getMaxX(), spModel.getMaxY()))*1000), -2000);
		rightPanel.add(smallFilterLabel);
		rightPanel.add(smallSliderPanel);


		smallSliderPanel.setLayout(new BoxLayout(smallSliderPanel, BoxLayout.X_AXIS));
		smallSliderPanel.add(smallTextField);

		/***
		 * Small Text Field
		 */
		smallTextField.setPreferredSize(new Dimension(20, 16));
		smallTextField.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e){
				int key = e.getKeyCode();
				if (key == KeyEvent.VK_ENTER) {
					try{
						double v = Double.parseDouble(smallTextField.getText());
						smallSlider.setValue((int) (v*1000));
					}
					catch (NumberFormatException e1){
						return ;
					}
				}
			}
		});
		smallTextField.setEditable(true);


		smallSlider.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent arg0) {
				// TODO Auto-generated method stub
				JSlider me = (JSlider)arg0.getSource();
				// TODO Auto-generated method stub
				smallFilter = me.getValue()/1000.0;
				me.setToolTipText(""+me.getValue());
				smallFilter = ((JSlider)arg0.getSource()).getValue()/1000.0;
				tableFilter.set(0, RowFilter.numberFilter(ComparisonType.AFTER, Math.pow(10, smallSlider.getValue()/1000.0), 1));
				tableFilter.set(1, RowFilter.numberFilter(ComparisonType.AFTER, Math.pow(10, smallSlider.getValue()/1000.0), 2));
				((TableRowSorter<TableModel>)detailTable.getRowSorter()).setRowFilter(RowFilter.andFilter(tableFilter));
				smallTextField.setText(""+me.getValue()/1000.0);


				me.repaint();//for refresh thumb value
			}
		});
		smallSlider.setPaintLabels(true);
		smallSlider.setPaintTicks(true);
		smallSlider.setMajorTickSpacing(1000);
		smallSlider.setUI(new BasicSliderUI(smallSlider) {
			  public void paintThumb(Graphics g) {
				    super.paintThumb(g);
				    g.setColor(Color.black);
				    g.drawString(Double.toString(slider.getValue()/1000.0), thumbRect.x, thumbRect.y + thumbRect.height+10);
				  }
				});
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		labelTable.put(new Integer(-1000), new JLabel("-1.0"));
		labelTable.put(new Integer(0), new JLabel("0.0"));
		labelTable.put(new Integer(1000), new JLabel("1.0"));
		labelTable.put(new Integer(2000), new JLabel("2.0"));
		labelTable.put(new Integer(3000), new JLabel("3.0"));
		labelTable.put(new Integer(4000), new JLabel("4.0"));
		labelTable.put(new Integer(5000), new JLabel("5.0"));

		smallSlider.setLabelTable(labelTable);
		smallSlider.setPreferredSize(new Dimension(280, 40));
		smallSliderPanel.add(smallSlider);





		JLabel equalFilterLabel = new JLabel("Difference Filter");
		final JTextField equalTextField = new JTextField();
		final JSlider equalSlider = new JSlider(0,
				(int)((isLogScale?(int)Math.log10(Math.max(spModel.getMaxX(), spModel.getMaxY())):(int) Math.max(spModel.getMaxX(), spModel.getMaxY()))*1000), 0);
		rightPanel.add(equalFilterLabel);
		JPanel equalPanel = new JPanel();
		equalPanel.setLayout(new BoxLayout(equalPanel, BoxLayout.X_AXIS));
		rightPanel.add(equalPanel);

		/***
		 * Equal Text Field
		 */
		equalTextField.setPreferredSize(new Dimension(20, 16));
		equalTextField.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e){
				int key = e.getKeyCode();
				if (key == KeyEvent.VK_ENTER) {
					try{
						double v = Double.parseDouble(equalTextField.getText());
						equalSlider.setValue((int) (v*1000));
					}
					catch (NumberFormatException e1){
						return ;
					}
				}
			}
		});
		equalTextField.setEditable(true);
		equalPanel.add(equalTextField);
		equalSlider.addChangeListener(new ChangeListener(){

			@Override
			public void stateChanged(ChangeEvent arg0) {
				JSlider me = (JSlider)arg0.getSource();
				// TODO Auto-generated method stub
				equalFilter = me.getValue()/1000.0;
				me.setToolTipText(""+me.getValue());
				tableFilter.set(2, RowFilter.numberFilter(ComparisonType.AFTER, equalSlider.getValue()/1000.0, 4));
				((TableRowSorter<TableModel>)detailTable.getRowSorter()).setRowFilter(RowFilter.andFilter(tableFilter));
				equalTextField.setText(""+me.getValue()/1000.0);
				me.repaint();//for thumb value
			}
		});
		equalSlider.setMajorTickSpacing(1000);
		equalSlider.setPaintLabels(true);
		equalSlider.setPaintTicks(true);
		equalSlider.setLabelTable(labelTable);
		equalSlider.setUI(new BasicSliderUI(equalSlider) {
			  public void paintThumb(Graphics g) {
				    super.paintThumb(g);
				    g.setColor(Color.black);
				    g.drawString(Double.toString(slider.getValue()/1000.0), thumbRect.x, thumbRect.y + thumbRect.height+10);
				  }
				});
		equalPanel.add(equalSlider);




		frame.add(centerPanel, BorderLayout.CENTER);
		frame.add(leftPanel, BorderLayout.WEST);
		frame.add(rightPanel, BorderLayout.EAST);
		frame.add(statusPanel, BorderLayout.SOUTH);
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
		FloatBuffer mat_specular = BufferUtils.createFloatBuffer(4);
		mat_specular.put(1);
		mat_specular.put(1);
		mat_specular.put(1);
		mat_specular.put(1);
		mat_specular.rewind();

		FloatBuffer light_position = BufferUtils.createFloatBuffer(4);
		light_position.put(10000);
		light_position.put(10000);
		light_position.put(10000);
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

		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, mat_specular);
		GL11.glMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT, mat_specular);
		GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, mat_specular);
		GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, mat_shininess);
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, light_position);
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, white_light);
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_AMBIENT, wlmodel_ambient);

		GL11.glEnable(GL11.GL_COLOR_MATERIAL);


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
			ExpressionData data = spModel.getDataTable().get(choose);
			if(Mouse.isButtonDown(0)){
				//TODO: highlight table row
				clickedIndex = overedIndex;
			}
			else{
				overedIndex = choose;
			}
			toolTipBox.setText(data.getName()+" ("+data.getX()+", "+data.getY()+")");
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
			ExpressionData e = spModel.getDataTable().get(clickedIndex);
			GL11.glColor3d(0, 0, 0);
			GL11.glBegin(GL11.GL_LINES);

			GL11.glVertex3d(e.getX(), e.getY(), e.getZ());
			GL11.glVertex3d(0, e.getY(), e.getZ());

			GL11.glVertex3d(0, e.getY(), e.getZ());
			GL11.glVertex3d(0, 0, e.getZ());

			GL11.glVertex3d(0, e.getY(), e.getZ());
			GL11.glVertex3d(0, e.getY(), 0);

			GL11.glVertex3d(e.getX(), e.getY(), e.getZ());
			GL11.glVertex3d(e.getX(), e.getY(), 0);

			GL11.glVertex3d(e.getX(), e.getY(), 0);
			GL11.glVertex3d(0, e.getY(), 0);

			GL11.glVertex3d(e.getX(), e.getY(), 0);
			GL11.glVertex3d(e.getX(), 0, 0);

			GL11.glVertex3d(e.getX(), e.getY(), e.getZ());
			GL11.glVertex3d(e.getX(), 0, e.getZ());

			GL11.glVertex3d(e.getX(), 0, e.getZ());
			GL11.glVertex3d(e.getX(), 0, 0);

			GL11.glVertex3d(e.getX(), 0, e.getZ());
			GL11.glVertex3d(0, 0, e.getZ());
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
		camera.move();

		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);

		GL11.glScaled(0.01, 0.01, 0.01);
		GL11.glScaled(100, 100, 100);

		GL11.glDisable(GL11.GL_TEXTURE_2D);

		drawDots();
		camera.getInput();

		if(renderMode != GL11.GL_SELECT){
			//drawShadows();
			drawAxis(maxXY);
			drawXYLine();
		}
	}


	private void drawDots() {
		GL11.glInitNames();
		GL11.glPointSize(7);
		for(int i = 0; i < totalSampleSize; i++){
			//TODO: dont use filter(). Instead, use table model

			if(!filter(spModel.getDataTable().get(i)))
				continue;
			double x, y;
			if(isLogScale){
				x = Math.log10((Double)detailTable.getModel().getValueAt(i, 1));
				y = Math.log10((Double)detailTable.getModel().getValueAt(i, 2));
			}
			else{
				x = (Double)detailTable.getModel().getValueAt(i, 1);
				y = (Double)detailTable.getModel().getValueAt(i, 2);
			}

			GL11.glPushName(i);
			GL11.glBegin(GL11.GL_POINTS);

			if(overedIndex == i||clickedIndex == i){
				GL11.glColor3f(0, 1, 1);
			}
			else{
				GL11.glColor3d(colorPalette.get(0).getRed()/255.0, colorPalette.get(0).getGreen()/255.0, colorPalette.get(0).getBlue()/255.0);
			}

			GL11.glVertex2d(x, y);
			GL11.glEnd();
			GL11.glPopName();
		}

	}
	private boolean filter(ExpressionData data){
		double x = data.getX();
		double y = data.getY();
		if(isLogScale){
			x = Math.log10(data.getX());
			y = Math.log10(data.getY());
		}
		if(x < smallFilter || y < smallFilter){
			return false;
		}
		else if(Math.abs(x - y) < equalFilter){
			return false;
		}
		return true;
	}
	private void drawXYLine(){
		GL11.glLineWidth(1);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex2d(0, 0);
		GL11.glVertex2d(maxXY, maxXY);
		GL11.glEnd();
	}
	private void drawAxis(double maxXY) {
		GL11.glLineWidth(1);
		GL11.glColor3d(0, 0, 0);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex2d(0, 0);
		GL11.glVertex2d(maxXY, 0);
		GL11.glVertex2d(0, 0);
		GL11.glVertex2d(0, maxXY);
		GL11.glEnd();


		Vector<Float[]> intersect = new Vector<Float[]>();
		float origin[] = new float[2];
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

		for(int i = 0; i < 2; i++){
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
					}
					if(win_pos.get(0) > 0 && win_pos.get(0) < viewport.get(2) && win_pos.get(1) > 0 && win_pos.get(1) < viewport.get(3)){
						xPos = (int) win_pos.get(0);
						yPos = viewport.get(3) - (int) win_pos.get(1);
					}
					axisLabel.setPosition(xPos-10, yPos);


				}
			}
		}
	}
}

