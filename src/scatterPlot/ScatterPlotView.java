package scatterPlot;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
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
	// UI initialize
	private final static AtomicReference<Dimension> newCanvasSize = new AtomicReference<Dimension>();
	boolean closeRequested = false;

	//camera
	private static Camera camera;

	//database
	static ScatterPlotModel spModel;
	JTable detailTable;
	int totalSampleSize = 0;
	int numOfshowingDots = 0;

	double maxXY;
	double maxX;
	double maxY;


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

	//labels
	Label xAxisLabel = new Label("X");
	Label yAxisLabel = new Label("Y");
	Label xyLabel = new Label("X = Y");
	Label xMaxLabel = new Label();
	Label yMaxLabel = new Label();

	//filtering values
	List<RowFilter<Object, Object>> tableFilter = new ArrayList<RowFilter<Object, Object>>(4);
	double smallFilter = log2(0.1);
	double equalFilter = 1;

	//is log scale
	boolean isLogScale = true;

	JLabel statusLabel;


	//for drawing
	Hashtable<String, Color> colormap = new Hashtable<String, Color>();

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
	private void updateMaxXY(){
		if(isLogScale){
			maxXY = Math.log(Math.max(spModel.getMaxX(), spModel.getMaxY()))/Math.log(2);
			maxX = Math.log(spModel.getMaxX())/Math.log(2);
			maxY = Math.log(spModel.getMaxY())/Math.log(2);
		}
		else{
			maxXY = Math.max(spModel.getMaxX(), spModel.getMaxY());
			maxX = spModel.getMaxX();
			maxY = spModel.getMaxY();
		}
	}
	public void start() {
		updateMaxXY();
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
			mainFrame.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					// TODO Auto-generated method stub
					controlFrame.setLocation(mainFrame.getX()+mainFrame.getWidth(), mainFrame.getY());
				}

				@Override
				public void componentMoved(ComponentEvent e) {
					controlFrame.setLocation(mainFrame.getX()+mainFrame.getWidth(), mainFrame.getY());
				}
			});

			controlFrame.setLocation(mainFrame.getX()+mainFrame.getWidth(), mainFrame.getY());
			controlFrame.pack();
			Display.create();

			GUI gui = initTWL();
			initOpenGL();
			makeColorMap();
			initFilters();

			LWJGLRenderer renderer = new LWJGLRenderer();

			Dimension newDim;

			while (!Display.isCloseRequested() && ! closeRequested){
				newDim = newCanvasSize.getAndSet(null);

				if(newDim != null){
					GL11.glViewport(0, 0, newDim.width, newDim.height);
					renderer.syncViewportSize();
				}

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
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
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
            toolTipBox.setBorderSize(1);
            xAxisLabel.setTheme("bigLabel");
            yAxisLabel.setTheme("bigLabel");
            xyLabel.setTheme("bigLabel");
            xMaxLabel.setTheme("label");
            yMaxLabel.setTheme("label");

    		add(toolTipBox);
    		add(xAxisLabel);
    		add(yAxisLabel);
    		add(xyLabel);
    		add(xMaxLabel);
    		add(yMaxLabel);
    		return gui;
		} catch (LWJGLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	private void initLayout() {
		controlFrame.setPreferredSize(new Dimension(600, 600));

		JPanel rightPanel = new JPanel();
		controlFrame.add(rightPanel);

		//frame.setLayout(new BorderLayout());



		canvas.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e)
			{ newCanvasSize.set(canvas.getSize()); }
		});
		mainFrame.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e){
				closeRequested = true;
			}
		});
		controlFrame.addWindowListener(new WindowAdapter(){
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
				JCheckBox me = (JCheckBox)e.getSource();
				isLogScale = me.isSelected();
				updateMaxXY();
			}
		});
		rightPanel.add(scaleCheckBox);


		JPanel smallSliderPanel = new JPanel();
		JLabel smallFilterLabel = new JLabel("RPKM Filter");
		final JTextField smallTextField = new JTextField(3);
		final JSlider smallSlider = new JSlider();
		if(isLogScale){
			smallSlider.setMaximum((int)(Math.log(spModel.getMax()+.1)/Math.log(2)*1000));
			smallSlider.setMinimum((int)(Math.log(.1)/Math.log(2)*1000));
			smallSlider.setValue(smallSlider.getMinimum());
		}
		else{
			smallSlider.setMaximum((int)(spModel.getMax()*1000));
			smallSlider.setMinimum(0);
			smallSlider.setValue(smallSlider.getMinimum());
		}
		rightPanel.add(smallSliderPanel);


		smallSliderPanel.setLayout(new FlowLayout());
		smallSliderPanel.add(smallFilterLabel);
		smallSliderPanel.add(smallTextField);

		/***
		 * Small Text Field
		 */
		smallTextField.setPreferredSize(new Dimension(100, 20));
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
				JSlider me = (JSlider)arg0.getSource();
				smallFilter = me.getValue()/1000.0-.1;
				me.setToolTipText(""+me.getValue());
				tableFilter.set(0, new RowFilter<Object, Object>(){
					public boolean include(Entry<? extends Object, ? extends Object> entry) {
						if(isLogScale){
							if(Math.log(Double.parseDouble(""+entry.getValue(1))+.1)/Math.log(2) > smallFilter &&
									Math.log(Double.parseDouble(""+entry.getValue(2))+.1)/Math.log(2) > smallFilter)
								return true;
							else
								return false;
						}
						else{
							if(Double.parseDouble(""+entry.getValue(1)) > smallFilter &&
									Double.parseDouble(""+entry.getValue(2)) > smallFilter)
								return true;
							else
								return false;
						}
					}
				});
				((TableRowSorter<TableModel>)detailTable.getRowSorter()).setRowFilter(RowFilter.andFilter(tableFilter));
				smallTextField.setText(""+me.getValue()/1000.0);

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
		rightPanel.add(smallSlider);

		scaleCheckBox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JCheckBox me = (JCheckBox)e.getSource();
				isLogScale = me.isSelected();
				if(isLogScale){
					smallSlider.setMaximum((int)(Math.log(spModel.getMax()+.1)/Math.log(2)*1000));
					smallSlider.setMinimum((int)(Math.log(.1)/Math.log(2)*1000));
					smallSlider.setValue(smallSlider.getMinimum());
				}
				else{
					smallSlider.setMaximum((int)(spModel.getMax()*1000));
					smallSlider.setMinimum(0);
					smallSlider.setValue(smallSlider.getMinimum());
				}
			}
		});


		JPanel equalPanel = new JPanel();
		equalPanel.setLayout(new FlowLayout());
		rightPanel.add(equalPanel);

		JLabel equalFilterLabel = new JLabel("Difference Filter");
		equalPanel.add(equalFilterLabel);
		final JTextField equalTextField = new JTextField();
		final JSlider equalSlider = new JSlider(1000, (int)(spModel.getMaxA()*1000), 1000);

		/***
		 * Equal Text Field
		 */
		equalTextField.setPreferredSize(new Dimension(100, 20));
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
				equalFilter = me.getValue()/1000.0;
				me.setToolTipText(""+equalFilter);
				tableFilter.set(1, new RowFilter<Object, Object>(){
					public boolean include(Entry<? extends Object, ? extends Object> entry) {
						double x = Double.parseDouble(""+entry.getValue(1));
						double y = Double.parseDouble(""+entry.getValue(2));

						if(Math.max(x, y)/Math.min(x, y) > equalFilter)
							return true;
						else
							return false;
					}
				});
				((TableRowSorter<TableModel>)detailTable.getRowSorter()).setRowFilter(RowFilter.andFilter(tableFilter));
				equalTextField.setText(""+equalFilter);
			}
		});
		equalSlider.setMajorTickSpacing(1000);
		equalSlider.setPaintLabels(true);
		equalSlider.setPaintTicks(true);
		equalSlider.setLabelTable(labelTable);
/*		equalSlider.setUI(new BasicSliderUI(equalSlider) {
			  public void paintThumb(Graphics g) {
				    super.paintThumb(g);
				    g.setColor(Color.black);
				    g.drawString(Double.toString(slider.getValue()/1000.0), thumbRect.x, thumbRect.y + thumbRect.height+10);
				  }
				});*/
		rightPanel.add(equalSlider);


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
	private void makeColorMap() {
		int alpha = 100;
		colormap.put("J", new Color(255, 0, 0, alpha));
		colormap.put("A", new Color(194, 175, 88, alpha));
		colormap.put("K", new Color(255, 153, 0, alpha));
		colormap.put("L", new Color(255, 255, 0, alpha));
		colormap.put("B", new Color(255, 198, 0, alpha));
		colormap.put("D", new Color(0x99, 0xff, 0, alpha));
		colormap.put("Y", new Color(0x49, 0x31, 0x26, alpha));
		colormap.put("V", new Color(0xff, 0x0, 0x8a, alpha));
		colormap.put("T", new Color(0, 0, 0xff, alpha));
		colormap.put("M", new Color(0x9e, 0xc9, 0x28, alpha));
		colormap.put("N", new Color(0x0, 0x66, 0x33, alpha));
		colormap.put("Z", new Color(0x66, 0, 0x99, alpha));
		colormap.put("W", new Color(0x33, 0x66, 0x99, alpha));
		colormap.put("U", new Color(0x33, 0xcc, 0x99, alpha));
		colormap.put("O", new Color(0x0, 0xff, 0xff, alpha));
		colormap.put("C", new Color(0x99, 0x00, 0xff, alpha));
		colormap.put("G", new Color(0x80, 0x56, 0x42, alpha));
		colormap.put("E", new Color(0xff, 0x0, 0xff, alpha));
		colormap.put("F", new Color(0x99, 0x33, 0x4d, alpha));
		colormap.put("H", new Color(0x72, 0x7d, 0xcc, alpha));
		colormap.put("I", new Color(0x5c, 0x5a, 0x1b, alpha));
		colormap.put("P", new Color(0x0, 0x99, 0xff, alpha));
		colormap.put("Q", new Color(0xff, 0xcc, 0x99, alpha));
		colormap.put("R", new Color(0xff, 0x99, 0x99, alpha));
		colormap.put("S", new Color(0xd6, 0xaa, 0xdf, alpha));
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
			drawFilterArea();
			drawMinMax();
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
				x = Math.log((Double)detailTable.getModel().getValueAt(i, 1)+0.1)/Math.log(2);
				y = Math.log((Double)detailTable.getModel().getValueAt(i, 2)+0.1)/Math.log(2);
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
				Color categoryColor = getColorByCategory(detailTable.getModel().getValueAt(i, 3).toString().substring(0, 1));
				GL11.glColor4d(categoryColor.getRed()/255.0, categoryColor.getGreen()/255.0, categoryColor.getBlue()/255.0, categoryColor.getAlpha()/255.0);
				GL11.glBegin(GL11.GL_POINTS);
				GL11.glVertex2d(x, y);
				GL11.glEnd();
			}
			GL11.glPopName();
		}

	}
	private Color getColorByCategory(String category){

		return colormap.get(category);

	}

	private void drawXYLine(){
		GL11.glLineWidth(2);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex2d(0, 0);
		GL11.glVertex2d(maxXY, maxXY);
		GL11.glEnd();
		int [] pos = getBoundary(maxXY, maxXY, 0);
		xyLabel.setPosition(pos[0]-50, pos[1]+50);

	}
	/***
	 * returns intersection point with boundaries
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	int []getBoundary(double x, double y, double z){
		int [] coord = Translater.getScreenCoordinate((float)x, (float)y, (float)z);
		if(coord[0] > Display.getWidth()){
			coord[0] = Display.getWidth();
		}
		else if(coord[0] < 0){
			coord[0] = 0;
		}
		if(coord[1] > Display.getHeight()){
			coord[1] = Display.getHeight();
		}
		else if(coord[1] < 0){
			coord[1] = 0;
		}
		coord[1] = Display.getHeight() - coord[1];

		return coord;
	}
	private void drawAxis(double maxXY) {
		GL11.glLineWidth(2);
		GL11.glColor3d(0, 0, 0);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex2d(0, 0);
		GL11.glVertex2d(maxXY*1.1, 0);
		GL11.glVertex2d(0, 0);
		GL11.glVertex2d(0, maxXY*1.1);
		GL11.glEnd();

		int xpos[] = getBoundary(maxXY*1.15, 0, 0);
		int ypos[] = getBoundary(0, maxXY*1.15, 0);

		xAxisLabel.setPosition(xpos[0]-10, xpos[1]+10);
		yAxisLabel.setPosition(ypos[0]-10, ypos[1]+10);

	}
	void drawFilterArea(){
		//draw small
		GL11.glColor4d(1, 0, 0, 0.2);
		GL11.glBegin(GL11.GL_QUADS);
		if(isLogScale){
			GL11.glVertex2d(log2(0.1), log2(0.1));
			GL11.glVertex2d(smallFilter, smallFilter);
			GL11.glVertex2d(log2(spModel.getMax()), smallFilter);
			GL11.glVertex2d(log2(spModel.getMax()), log2(.1));

			GL11.glVertex2d(log2(0.1), log2(0.1));
			GL11.glVertex2d(smallFilter, smallFilter);
			GL11.glVertex2d(smallFilter, log2(spModel.getMax()));
			GL11.glVertex2d(log2(.1), log2(spModel.getMax()));

		}
		else{
			GL11.glVertex2d(0, 0);
			GL11.glVertex2d(smallFilter, smallFilter);
			GL11.glVertex2d(spModel.getMax(), smallFilter);
			GL11.glVertex2d(spModel.getMax(), 0);

			GL11.glVertex2d(0, 0);
			GL11.glVertex2d(smallFilter, smallFilter);
			GL11.glVertex2d(smallFilter, spModel.getMax());
			GL11.glVertex2d(0, spModel.getMax());
		}
		GL11.glEnd();


		//draw equal
		GL11.glColor3d(0.8, 0.8, 1);
		GL11.glBegin(GL11.GL_TRIANGLES);
		if(isLogScale){
			double prev = 2;
			for(double i = 2; i < spModel.getMax(); i+= 2){
				GL11.glVertex2d(log2(0.1), log2(0.1));
				GL11.glVertex2d(log2(prev), log2(prev*equalFilter));
				GL11.glVertex2d(log2(i), log2(i*equalFilter));

				GL11.glVertex2d(log2(0.1), log2(0.1));
				GL11.glVertex2d(log2(prev*equalFilter), log2(prev));
				GL11.glVertex2d(log2(i*equalFilter), log2(i));

				prev = i;

			}
			GL11.glVertex2d(log2(0.1), log2(0.1));
			GL11.glVertex2d(log2(spModel.getMax()), log2(spModel.getMax()));
			GL11.glVertex2d(log2(spModel.getMax()), log2(spModel.getMax()*equalFilter));
			GL11.glVertex2d(log2(0.1), log2(0.1));
			GL11.glVertex2d(log2(spModel.getMax()), log2(spModel.getMax()));
			GL11.glVertex2d(log2(spModel.getMax()*equalFilter), log2(spModel.getMax()));

		}
		else{
			GL11.glVertex2d(0, 0);
			GL11.glVertex2d(spModel.getMax(), spModel.getMax());
			GL11.glVertex2d(spModel.getMax(), spModel.getMax()*equalFilter);

			GL11.glVertex2d(0, 0);
			GL11.glVertex2d(spModel.getMax(), spModel.getMax());
			GL11.glVertex2d(spModel.getMax(), spModel.getMax()/equalFilter);
		}
		GL11.glEnd();

	}
	private double log2(double a){
		return Math.log(a)/Math.log(2);
	}
	private void drawMinMax(){
		GL11.glColor3d(0, 0, 0);
		int xpos[] = Translater.getScreenCoordinate((float) maxX, 0, 0);
		xMaxLabel.setText(spModel.getMaxX()+"");
		xMaxLabel.setPosition(xpos[0], Display.getHeight()-xpos[1]+xMaxLabel.getPreferredHeight());
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex2d(maxX, -.1);
		GL11.glVertex2d(maxX, .1);
		GL11.glEnd();

		int ypos[] = Translater.getScreenCoordinate(0, (float) maxY, 0);
		yMaxLabel.setText(spModel.getMaxY()+"");
		yMaxLabel.setPosition(ypos[0] - yMaxLabel.getPreferredWidth(), Display.getHeight()-ypos[1]);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex2d(-.1, maxY);
		GL11.glVertex2d(.1, maxY);
		GL11.glEnd();


	}
}

