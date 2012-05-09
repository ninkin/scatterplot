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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
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
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.SplitPane;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.renderer.lwjgl.LWJGLRenderer;
import de.matthiasmann.twl.theme.ThemeManager;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
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
	JFrame mainFrame = new JFrame("RPKM Scatterplot");
	JFrame controlFrame = new JFrame("Control Frame");
	final Canvas canvas = new Canvas();

	//axis
	Label xAxisLabel = new Label("X");
	Label yAxisLabel = new Label("Y");

	//filtering values
	List<RowFilter<Object, Object>> tableFilter = new ArrayList<RowFilter<Object, Object>>(4);

	//is log scale
	boolean isLogScale = true;

	JLabel statusLabel;

	public static void main(String[] argv) {
		spModel = new ScatterPlotModel();
		spModel.readTXTData("RPKM.txt", "Feature ID", "RPKM1", "RPKM2", "COG Category");

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
			maxXY = Math.log(Math.max(spModel.getMaxX(), spModel.getMaxY()))/Math.log(2);
		}
		else{
			maxXY = Math.max(spModel.getMaxX(), spModel.getMaxY());
		}
		camera = new Camera(-maxXY/10, 1.1*maxXY, -maxXY/10, 1.1*maxXY, 1, -1);
		initLayout();
		try {
			canvas.setVisible(true);
			mainFrame.setVisible(true);
			controlFrame.setVisible(true);
			Display.setParent(canvas);
			Display.setResizable(true);
			Display.setVSyncEnabled(true);
			mainFrame.pack();

			controlFrame.setLocation(mainFrame.getX()+mainFrame.getWidth(), mainFrame.getY());
			controlFrame.pack();
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
		mainFrame.dispose();

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
		controlFrame.setPreferredSize(new Dimension(600, 600));

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		mainFrame.add(centerPanel);



		JPanel rightPanel = new JPanel();
		controlFrame.add(rightPanel);

		//frame.setLayout(new BorderLayout());



		canvas.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e)
			{ newCanvasSize.set(canvas.getSize()); }
		});

		mainFrame.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent e)
			{ canvas.requestFocusInWindow(); }
		});
		mainFrame.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e){
				closeRequested = true;
			}
		});

		mainFrame.add(canvas, BorderLayout.CENTER);
		canvas.setPreferredSize(new java.awt.Dimension(600, 600));

		JPanel statusPanel = new JPanel();
		statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));

		statusLabel = new JLabel();
		statusLabel.setPreferredSize(new Dimension(600, 16));

		statusPanel.add(statusLabel);
		mainFrame.add(statusPanel, BorderLayout.SOUTH);

		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		detailTable = new JTable(spModel.getDataTable(), spModel.getColumnNames());

		detailTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				JTable me = ((JTable)e.getSource());
				clickedIndex = me.convertRowIndexToModel(me.getSelectedRow());
			}
		});
		totalSampleSize = detailTable.getRowCount();
		detailTable.setRowSorter(new TableRowSorter<TableModel>(detailTable.getModel()));
		detailTable.getRowSorter().toggleSortOrder(3);
		JScrollPane tablePane = new JScrollPane(detailTable);
		rightPanel.add(tablePane);

		JCheckBox scaleCheckBox = new JCheckBox("Log Scale");
		scaleCheckBox.setSelected(true);
		scaleCheckBox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// TODO Auto-generated method stub
				JCheckBox me = (JCheckBox)e.getSource();
				isLogScale = me.isSelected();
			}
		});
		rightPanel.add(scaleCheckBox);


		JPanel smallSliderPanel = new JPanel();
		JLabel smallFilterLabel = new JLabel("RPKM Filter");
		final JTextField smallTextField = new JTextField(3);
		final JSlider smallSlider = new JSlider();
		if(isLogScale){
			smallSlider.setMaximum((int)(Math.log(spModel.getMax())/Math.log(2)*1000));
			smallSlider.setMinimum((int)(Math.log(spModel.getMin())/Math.log(2)*1000));
			smallSlider.setValue(smallSlider.getMinimum());
		}
		else{
			smallSlider.setMaximum((int)spModel.getMax());
			smallSlider.setMinimum(0);
			smallSlider.setValue(0);
		}
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
		smallTextField.setFocusable(true);


		smallSlider.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent arg0) {
				// TODO Auto-generated method stub
				JSlider me = (JSlider)arg0.getSource();
				// TODO Auto-generated method stub
				double smallFilter = me.getValue()/1000.0;
				me.setToolTipText(""+me.getValue());
				tableFilter.set(0, RowFilter.numberFilter(ComparisonType.AFTER, Math.exp(smallFilter*Math.log(2)), 1));
				tableFilter.set(1, RowFilter.numberFilter(ComparisonType.AFTER, Math.exp(smallFilter*Math.log(2)), 2));
				((TableRowSorter<TableModel>)detailTable.getRowSorter()).setRowFilter(RowFilter.andFilter(tableFilter));
				smallTextField.setText(""+me.getValue()/1000.0);


				me.repaint();//for refresh thumb value
			}
		});

		smallSlider.setPaintLabels(true);
		smallSlider.setPaintTicks(true);
/*		smallSlider.setUI(new BasicSliderUI(smallSlider) {
			  public void paintThumb(Graphics g) {
				    super.paintThumb(g);
				    g.setColor(Color.black);
				    g.drawString(Double.toString(slider.getValue()/1000.0), thumbRect.x, thumbRect.y + thumbRect.height+10);
				  }
				});*/
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

		scaleCheckBox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {

				// TODO Auto-generated method stub
				JCheckBox me = (JCheckBox)e.getSource();
				isLogScale = me.isSelected();
				if(isLogScale){
					smallSlider.setMaximum((int)(Math.log(spModel.getMax())/Math.log(2)*1000));
					smallSlider.setMinimum((int)(Math.log(spModel.getMin())/Math.log(2)*1000));
					smallSlider.setValue(smallSlider.getMinimum());
					smallSlider.setMajorTickSpacing(1000);
				}
				else{
					smallSlider.setMaximum((int)spModel.getMax()*1000);
					smallSlider.setMinimum(0);
					smallSlider.setValue(0);
					smallSlider.setMajorTickSpacing(1000000);
				}
			}
		});



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
				double equalFilter = me.getValue()/1000.0;
				me.setToolTipText(""+equalFilter);
				tableFilter.set(2, RowFilter.numberFilter(ComparisonType.AFTER, equalFilter, 4));
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


	}
	private void initOpenGL() {

		selectBuff = BufferUtils.createIntBuffer(1024);
		GL11.glSelectBuffer(selectBuff);

		GL11.glClearColor(1, 1, 1, 1);

		GL11.glEnable(GL11.GL_NORMALIZE);

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
			ExpressionData data = spModel.getDataTable().get(choose);
			if(Mouse.isButtonDown(0)){
				System.out.println(hits);
				//TODO: highlight table row
				clickedIndex = overedIndex;
				int index = detailTable.convertRowIndexToView(choose);
				detailTable.getSelectionModel().setSelectionInterval(index, index);
				detailTable.scrollRectToVisible(detailTable.getCellRect(index, 0, true));
				detailTable.repaint();
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
	private void display(){
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();


		if(renderMode == GL11.GL_SELECT){
			IntBuffer viewport = BufferUtils.createIntBuffer(16);
			GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
			GLU.gluPickMatrix(Mouse.getX(), Mouse.getY(), 3, 3, viewport);
		}
		camera.move();

		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);


		drawDots();
		camera.getInput();

		if(renderMode != GL11.GL_SELECT){
			//drawShadows();
			if(isLogScale){
				drawAxis(Math.log(Math.max(spModel.getMaxX(), spModel.getMaxX()))/Math.log(2));
			}
			else{
				drawAxis(Math.max(spModel.getMaxX(), spModel.getMaxX()));
			}

			drawXYLine();
		}
	}
	private void drawDots() {
		GL11.glInitNames();


		for(int i = 0; i < totalSampleSize; i++){
			if(detailTable.convertRowIndexToView(i) == -1)
				continue;//i is not shown
			double x, y;
			if(isLogScale){
				x = Math.log((Double)detailTable.getModel().getValueAt(i, 1))/Math.log(2);
				y = Math.log((Double)detailTable.getModel().getValueAt(i, 2))/Math.log(2);
			}
			else{
				x = (Double)detailTable.getModel().getValueAt(i, 1);
				y = (Double)detailTable.getModel().getValueAt(i, 2);
			}

			GL11.glPushName(i);

			if(overedIndex == i||clickedIndex == i){
				GL11.glPointSize(14);
				GL11.glColor3f(0, 0, 0);
				GL11.glBegin(GL11.GL_POINTS);
				GL11.glVertex3d(x, y, 0.5);
				GL11.glEnd();
			}
			else{
				GL11.glPointSize(7);
				Color categoryColor = getColorByCategory(detailTable.getModel().getValueAt(i, 3).toString().charAt(0));
				GL11.glColor4d(categoryColor.getRed()/255.0, categoryColor.getGreen()/255.0, categoryColor.getBlue()/255.0, categoryColor.getAlpha()/255.0);
				GL11.glBegin(GL11.GL_POINTS);
				GL11.glVertex2d(x, y);
				GL11.glEnd();
			}
			GL11.glPopName();
		}

	}
	private Color getColorByCategory(char category){
		int alpha = 255;
		if(category=='J'){
			return new Color(255, 0, 0, alpha);
		}
		else if(category=='A'){
			return new Color(194, 175, 88, alpha);
		}
		else if(category=='K'){
			return new Color(255, 153, 0, alpha);
		}
		else if(category=='L'){
			return new Color(255, 255, 0, alpha);
		}
		else if(category =='B'){
			return new Color(255, 198, 0, alpha);
		}
		else if(category=='D'){
			return new Color(0x99, 0xff, 0, alpha);
		}
		else if(category =='Y'){
			return new Color(0x49, 0x31, 0x26, alpha);
		}
		else if(category =='V'){
			return new Color(0xff, 0x0, 0x8a, alpha);
		}
		else if(category =='T'){
			return new Color(0, 0, 0xff, alpha);
		}
		else if(category =='M'){
			return new Color(0x9e, 0xc9, 0x28, alpha);
		}
		else if(category =='N'){
			return new Color(0x0, 0x66, 0x33, alpha);
		}
		else if(category =='Z'){
			return new Color(0x66, 0, 0x99, alpha);
		}
		else if(category =='W'){
			return new Color(0x33, 0x66, 0x99, alpha);
		}
		else if(category =='U'){
			return new Color(0x33, 0xcc, 0x99, alpha);
		}
		else if(category =='O'){
			return new Color(0x0, 0xff, 0xff, alpha);
		}
		else if(category =='C'){
			return new Color(0x99, 0, 0xff, alpha);
		}
		else if(category =='G'){
			return new Color(0x80, 0x56, 0x42, alpha);
		}
		else if(category =='E'){
			return new Color(0xff, 0, 0xff, alpha);
		}
		else if(category =='F'){
			return new Color(0x99, 0x33, 0x4d, alpha);
		}
		else if(category =='H'){
			return new Color(0x72, 0x7d, 0xcc, alpha);
		}
		else if(category =='I'){
			return new Color(0x5c, 0x5a, 0x1b, alpha);
		}
		else if(category =='P'){
			return new Color(0, 0x99, 0xff, alpha);
		}
		else if(category =='Q'){
			return new Color(0xff, 0xcc, 0x99, alpha);
		}
		else if(category =='R'){
			return new Color(0xff, 0x99, 0x99, alpha);
		}
		else if(category =='S'){
			return new Color(0xd6, 0xaa, 0xdf, alpha);
		}
		else{
			return Color.black;
		}
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

