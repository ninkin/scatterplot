package scatterPlot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PPanEventHandler;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PDimension;

public class ScatterPlotView {

	class DiffComp implements Comparator<DoubleAndInt> {
		@Override
		public int compare(DoubleAndInt o1, DoubleAndInt o2) {
			double d11 = o1.d.get(xIndex);
			double d12 = o1.d.get(yIndex);
			double d21 = o2.d.get(xIndex);
			double d22 = o2.d.get(yIndex);
			double a1, a2;
			if (d11 == d12) {
				a1 = 1;
			} else {
				a1 = Math.max(d11, d12) / Math.min(d11, d12);
			}

			if (d21 == d22) {
				a2 = 1;
			} else {
				a2 = Math.max(d21, d22) / Math.min(d21, d22);
			}

			if (a1 <= a2) {
				return -1;
			} else {
				return 1;
			}
		}
	}
	class DotFilteringWorker implements Runnable {
		public boolean terminationRequested = false;

		@Override
		public void run() {
			terminationRequested = false;
			int visibledots = 0;
			for (int i = 0; i < dots.size() && !terminationRequested; i++) {
				int index = Integer.parseInt(dots.get(i).getAttribute("index")
						.toString());
				int isFiltered = detailTable.getRowSorter()
						.convertRowIndexToView(index);
				if (isFiltered == -1) {
					dots.get(i).setVisible(false);
				} else {
					dots.get(i).setVisible(true);
					dots.get(i).repaint();
					visibledots++;
				}
			}
			statusLabel.setText(String.format("%d / %d", visibledots,
					dots.size()));
		}

		public void setTerminationRequested(boolean terminationRequested) {
			this.terminationRequested = terminationRequested;
		}
	}
	class DotInputListener extends PBasicInputEventHandler {
		@Override
		public void mouseClicked(PInputEvent event) {
			if (event.getPickedNode() instanceof PPath) {
				PPath dot = (PPath) event.getPickedNode();
				int i = detailTable.convertRowIndexToView((Integer) dot
						.getAttribute("index"));
				detailTable.getSelectionModel().setSelectionInterval(i, i);
				detailTable.scrollRectToVisible(detailTable.getCellRect(i, 0,
						true));
				detailTable.repaint();
			}
		}

		@Override
		public void mouseEntered(PInputEvent event) {
			if (event.getPickedNode() instanceof PPath) {
				PPath dot = (PPath) event.getPickedNode();
				dot.setPathToEllipse(
						(float) (dot.getX() + dot.getWidth() / 2 - hoveredRadius),
						(float) (dot.getY() + dot.getHeight() / 2 - hoveredRadius),
						2 * hoveredRadius, 2 * hoveredRadius);
				dot.moveToFront();
				dot.setPaint(hoveredColor);
				tooltip.setText(dot.getName());
				Point2D p = event.getCanvasPosition();
				tooltip.setOffset(p.getX() + 10, p.getY() + 10);
				tooltip.setVisible(true);
			}
		};

		@Override
		public void mouseExited(PInputEvent event) {
			if (event.getPickedNode() instanceof PPath) {
				PPath dot = (PPath) event.getPickedNode();
				dot.setPathToEllipse(
						(float) (dot.getX() + dot.getWidth() / 2 - normalRadius),
						(float) (dot.getY() + dot.getHeight() / 2 - normalRadius),
						2 * normalRadius, 2 * normalRadius);
				dot.setPaint((Paint) dot.getAttribute("color"));
				tooltip.setVisible(false);
			}
		}
	}

	class DoubleAndInt {
		public List<Double> d;
		public int i;
	}
	class SmallComp implements Comparator<DoubleAndInt> {

		@Override
		public int compare(DoubleAndInt o1, DoubleAndInt o2) {
			double min1 = Math.min(o1.d.get(xIndex), o1.d.get(yIndex));
			double min2 = Math.min(o2.d.get(xIndex), o2.d.get(yIndex));
			if (min1 < min2) {
				return -1;
			} else {
				return 1;
			}
		}
	}
	class TickHandler implements ActionListener {
		List<PPath> ticks;
		double interval;
		Option opt = new Option(mainFrame, "Option", true);

		public TickHandler(List<PPath> ticks) {
			this.ticks = ticks;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			opt.setVisible(true);
			interval = opt.tickInterval;
			if (opt.isOK) {
				if (opt.showTick) {
					drawTicks();
				} else {
					removeTicks();
				}
			}
		}

		public void drawTicks() {
			removeTicks();

			Dimension dim = canvas.getPreferredSize();
			double x = 0, y;
			for (int i = 0; i * interval < maxX; i++) {
				if (isLogScale) {
					x = log2(i * interval + 0.1) / (max - min) * dim.width;
					y = dim.getHeight();
				} else {
					x = i * interval / (max - min) * dim.width;
					y = dim.getHeight();
				}
				PPath tick1 = PPath.createLine((float) x, (float) y - 5,
						(float) x, (float) y + 5);
				ticks.add(tick1);
				axisLayer.addChild(tick1);

				PPath tick2 = PPath.createLine(-5, dim.height - (float) x, 5,
						dim.height - (float) x);
				ticks.add(tick2);
				axisLayer.addChild(tick2);
			}
			axisLayer.invalidatePaint();
		}

		public void removeTicks() {
			for (int i = 0; i < ticks.size(); i++) {
				ticks.get(i).removeFromParent();
			}
			ticks.clear();
		}

		public void resetTicks() {
			if (opt.showTick) {
				drawTicks();
			} else {
				removeTicks();
			}
		}
	}
	// database
	static ScatterPlotModel spModel;

	JTable detailTable;
	Vector<String> selectedItems = new Vector<String>();

	double max;
	double min;
	double maxX;
	double maxY;

	HTooltipNode tooltip = new HTooltipNode();
	FileOpenDialog fileDialog = new FileOpenDialog();
	int xIndex = 0;

	int yIndex = 1;
	JComboBox<String> xColumnList;
	JComboBox<String> yColumnList;
	// layouts
	JMenuBar menuBar;

	JFrame mainFrame = new JFrame("RPKM Scatterplot");
	final JPanel canvasPanel = new JPanel();
	// labels
	PText xAxisLabel = new PText("X");

	PText yAxisLabel = new PText("Y");
	PText xMaxLabel = new PText();

	PText yMaxLabel = new PText();
	// filtering
	List<RowFilter<Object, Object>> tableFilter = new ArrayList<RowFilter<Object, Object>>(
			4);
	double smallFilter = log2(0.1);
	double equalFilter = 1;
	DotFilteringWorker filteringWorker = new DotFilteringWorker();

	Thread t = new Thread(filteringWorker);

	JSlider equalSlider;

	JSlider smallSlider;
	JCheckBox scaleCheckBox;
	JPanel rightPanel;
	JPopupMenu canvasPopup;
	// is log scale
	boolean isLogScale = true;
	JLabel statusLabel;

	PCanvas canvas = new PCanvas();

	PLayer dotLayer;
	PLayer axisLayer;
	List<PPath> dots;
	List<PPath> ticks = new ArrayList<PPath>();
	TickHandler tickHandler = new TickHandler(ticks);

	// for drawing
	Hashtable<String, Color> colormap = new Hashtable<String, Color>();

	int clickedIndex = -1;

	private Color clickedColor = Color.black;

	private Color hoveredColor = Color.black;

	private int normalRadius = 2;

	private int hoveredRadius = 5;

	public ScatterPlotView(ScatterPlotModel model) {
		spModel = model;
	}

	private void drawEverything() {
		if (dotLayer != null) {
			canvas.getCamera().removeLayer(dotLayer);
		}
		if (axisLayer != null) {
			canvas.getCamera().removeLayer(axisLayer);
		}
		dotLayer = getDotsLayer();
		axisLayer = getAxisLayer();
		canvas.getCamera().addLayer(axisLayer);
		canvas.getCamera().addLayer(dotLayer);
		canvas.getCamera().addChild(tooltip);
		Dimension dim = canvas.getPreferredSize();
		canvas.getCamera().translateView(dim.getWidth() / 10,
				-dim.getHeight() / 10);
		canvas.getCamera().addAttribute("initialTransform",
				canvas.getCamera().getViewTransform());

	}

	private double exp2(double v) {
		return Math.exp((Math.log(2) * v));
	}

	// private void resort() {
	// Collections.sort(smallSortedDots, new SmallComp());
	// Collections.sort(diffSortedDots, new DiffComp());
	// }

	synchronized void fileChanged() {
		updateMinXY();
		dots.clear();
		xColumnList.setModel(new DefaultComboBoxModel<String>(spModel
				.getDataColumnNames()));
		xColumnList.setSelectedIndex(0);
		yColumnList.setModel(new DefaultComboBoxModel<String>(spModel
				.getDataColumnNames()));
		yColumnList.setSelectedIndex(1);
		scaleCheckBox.setSelected(true);
		equalSlider.setMaximum((int) (spModel.getMaxA() * 1000));
		equalSlider.setMinimum(1000);
		equalSlider.setValue(1000);
		smallSlider.setMaximum((int) (Math.log(spModel.getMax() + .1)
				/ Math.log(2) * 1000));
		smallSlider.setMinimum((int) (Math.log(.1) / Math.log(2) * 1000));
		smallSlider.setValue(smallSlider.getMinimum());
		TableModel detailTableModel = new NonEditableTableModel(
				spModel.getDataTable(), spModel.getColumnNames());
		detailTable.setModel(detailTableModel);
		detailTable.setRowSorter(getRowSorter(detailTable));
		((TableRowSorter<TableModel>) detailTable.getRowSorter())
				.setRowFilter(RowFilter.andFilter(tableFilter));
		for (Component c : rightPanel.getComponents()) {
			if (c instanceof PCanvas) {
				rightPanel.remove(c);
				rightPanel.add(getHistogram());
			}
		}
		drawEverything();
	}

	double[] getAdjustedLocation(ExpressionData data, boolean isLogScale) {
		double x, y;
		if (isLogScale) {
			x = log2(data.getExp(xIndex) + 0.1);
			y = log2(data.getExp(yIndex) + 0.1);
		} else {
			x = data.getExp(xIndex);
			y = data.getExp(yIndex);
		}
		return new double[] { x, y };
	}

	private PLayer getAxisLayer() {
		PLayer layer = new PLayer();
		layer.addChild(xAxisLabel);
		layer.addChild(yAxisLabel);
		Dimension dim = canvas.getPreferredSize();
		xAxisLabel.setOffset(dim.getWidth() - xAxisLabel.getWidth(),
				dim.getHeight());
		yAxisLabel.rotate(-Math.PI / 2);
		yAxisLabel.setHorizontalAlignment(Component.RIGHT_ALIGNMENT);
		layer.addChild(PPath.createLine(0, 0, 0, 2 * (float) dim.getHeight()));
		layer.addChild(PPath.createLine(-(float) dim.getWidth(),
				(float) dim.getHeight(), (float) dim.getWidth(),
				(float) dim.getHeight()));
		layer.addChild(PPath.createLine(-(float) dim.getWidth(),
				2 * (float) dim.getHeight(), (float) dim.getWidth(), 0));
		PText xyLine = new PText("Y = X");
		xyLine.setOffset(dim.getWidth() - 2 * xyLine.getWidth(), 0);
		layer.addChild(xyLine);

		layer.addChild(yMaxLabel);
		layer.addChild(xMaxLabel);
		return layer;
	}

	private JPopupMenu getCanvasPopup() {
		JPopupMenu popup = new JPopupMenu();
		JMenuItem sample = new JMenuItem("Sample");
		popup.add(sample);

		return popup;
	}

	private Color getColorByCategory(String category) {
		if (category.length() > 1) {
			category = category.substring(0, 1);
		}

		return colormap.get(category);
	}

	private PLayer getDotsLayer() {
		PLayer layer = new PLayer();
		DotInputListener listener = new DotInputListener();
		layer.addInputEventListener(listener);

		dots = new ArrayList<PPath>(spModel.getDataTable().size());

		Dimension2D dim = canvas.getPreferredSize();
		for (int i = 0; i < spModel.getDataTable().size(); i++) {
			ExpressionData data = spModel.getDataTable().get(i);

			double[] xy = getAdjustedLocation(data, isLogScale);
			PPath dot = PPath
					.createEllipse(
							(float) (xy[0] / (max - min) * dim.getWidth() - normalRadius),
							(float) (dim.getHeight()
									- (xy[1] / (max - min) * dim.getHeight()) - normalRadius),
							2 * normalRadius, 2 * normalRadius);
			dot.setStroke(null);
			Color categoryColor = getColorByCategory(detailTable.getModel()
					.getValueAt(i, detailTable.getModel().getColumnCount() - 1)
					.toString().substring(0, 1));

			if (categoryColor == null) {
				categoryColor = Color.black;
			}
			dot.setName(spModel.getDataTable().get(i).getName());
			dot.addAttribute("index", i);
			dot.addAttribute("data", data);
			dot.addAttribute("color", categoryColor);
			dot.setPaint(categoryColor);
			dots.add(dot);
			DoubleAndInt di = new DoubleAndInt();
			di.d = new ArrayList<Double>();
			for (int j = 0; j < spModel.getDataColumnNames().size(); j++) {
				double d = data.getExp(j);
				di.d.add(d);
			}
			di.i = i;

			// smallSortedDots.add(di);
			// diffSortedDots.add(di);

			layer.addChild(dot);
		}
		// resort();
		return layer;
	}

	private JPanel getEqualFilterPanel() {
		JPanel equalPanel = new JPanel();
		equalPanel.setLayout(new BoxLayout(equalPanel, BoxLayout.Y_AXIS));

		JPanel equalSubPanel = new JPanel();
		equalSubPanel.setLayout(new FlowLayout());

		equalPanel.add(equalSubPanel);

		JLabel equalFilterLabel = new JLabel("Difference Filter");
		equalFilterLabel.setPreferredSize(new Dimension(100, 20));
		equalSubPanel.add(equalFilterLabel);
		final IntegerTextField equalTextField = new IntegerTextField();
		equalSlider = new JSlider(1000, (int) (spModel.getMaxA() * 1000), 1000);

		/***
		 * Equal Text Field
		 */
		equalTextField.setPreferredSize(new Dimension(100, 20));
		equalTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		equalTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int key = e.getKeyCode();
				if (key == KeyEvent.VK_ENTER) {
					try {
						double v = Double.parseDouble(equalTextField.getText());
						equalSlider.setValue((int) (v * 1000));
					} catch (NumberFormatException e1) {
						return;
					}
				}
			}
		});
		equalTextField.setEditable(true);
		equalSubPanel.add(equalTextField);

		equalSlider.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				final JSlider me = (JSlider) e.getSource();
				me.setToolTipText("" + equalFilter);
				equalTextField.setText("" + me.getValue() / 1000.0);
			}
		});
		equalSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				JSlider me = (JSlider) arg0.getSource();
				double prevEqual = equalFilter;
				equalFilter = me.getValue() / 1000.0;

				DoubleAndInt dummy = new DoubleAndInt();

				int insertionPoint1;
				int insertionPoint2;

				// dummy.d = new ArrayList<Double>();
				// dummy.d.add(1d);
				// for (int i = 1; i < spModel.getDataColumnNames().size(); i++)
				// {
				// dummy.d.add(prevEqual);
				// }
				// dummy.i = -1;
				// insertionPoint1 = Collections.binarySearch(diffSortedDots,
				// dummy, new DiffComp());
				// if (insertionPoint1 < 0) {
				// insertionPoint1 = -(insertionPoint1 + 1);
				// }
				//
				// dummy.d = new ArrayList<Double>();
				// dummy.d.add(1d);
				// for (int i = 1; i < spModel.getDataColumnNames().size(); i++)
				// {
				// dummy.d.add(equalFilter);
				// }
				//
				// insertionPoint2 = Collections.binarySearch(diffSortedDots,
				// dummy, new DiffComp());
				// if (insertionPoint2 < 0) {
				// insertionPoint2 = -(insertionPoint2 + 1);
				// }
				// for (int i = 0; i < insertionPoint2; i++) {
				// dots.get(smallSortedDots.get(i).i).setVisible(false);
				// dots.get(smallSortedDots.get(i).i)
				// .setPaintInvalid(true);
				// }
				// for (int i = insertionPoint2; i < dots.size(); i++) {
				// dots.get(smallSortedDots.get(i).i).setVisible(true );
				// dots.get(smallSortedDots.get(i).i).repaint();
				// }
				// if (insertionPoint1 < insertionPoint2) {
				// for (int i = insertionPoint1; i < insertionPoint2; i++) {
				// dots.get(smallSortedDots.get(i).i).setVisible(false);
				// dots.get(smallSortedDots.get(i).i)
				// .setPaintInvalid(true);
				// }
				// } else {
				// for (int i = insertionPoint2; i < insertionPoint1; i++) {
				// dots.get(smallSortedDots.get(i).i).setVisible(true);
				// dots.get(smallSortedDots.get(i).i).repaint();
				// }
				// }
				try {
					filteringWorker.setTerminationRequested(true);
					t.join();
					t = new Thread(filteringWorker);
					t.start();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				equalTextField.setText(equalFilter + "");
				tableFilter.set(1, new RowFilter<Object, Object>() {
					@Override
					public boolean include(
							Entry<? extends Object, ? extends Object> entry) {
						double x = Double.parseDouble(""
								+ entry.getValue(xIndex + 1));
						double y = Double.parseDouble(""
								+ entry.getValue(yIndex + 1));
						double a;
						if (x == y) {
							a = 1;
						} else {
							a = Math.max(x, y) / Math.min(x, y);
						}

						if (a >= equalFilter)
							return true;
						else
							return false;
					}
				});
				((TableRowSorter<TableModel>) detailTable.getRowSorter())
						.setRowFilter(RowFilter.andFilter(tableFilter));
			}
		});
		equalSlider.setMajorTickSpacing(1000);
		equalSlider.setPaintLabels(true);
		equalSlider.setPaintTicks(true);
		final Hashtable<Integer, JLabel> equalLabelTable = new Hashtable<Integer, JLabel>();
		for (int i = 1; i < spModel.getMaxA(); i++) {
			equalLabelTable.put(new Integer(i * 1000), new JLabel(i + ""));
		}
		equalSlider.setLabelTable(equalLabelTable);
		equalPanel.add(equalSlider);
		return equalPanel;
	}

	private PCanvas getHistogram() {
		PCanvas histogramView = new PCanvas();

		histogramView.setPanEventHandler(new PPanEventHandler() {
			// override pan method for horizontal only scrolling
			@Override
			protected void pan(PInputEvent event) {
				PCamera c = event.getCamera();
				Point2D l = event.getPosition();
				if (c.getViewBounds().contains(l)) {
					PDimension d = event.getDelta();
					c.translateView(d.getWidth(), 0);
				}
			};
		});
		histogramView.getPanEventHandler().setAutopan(false);

		final PText tooltipNode = new PText();
		final PCamera pcamera = histogramView.getCamera();

		tooltipNode.setPickable(false);
		pcamera.addChild(tooltipNode);

		pcamera.addInputEventListener(new PBasicInputEventHandler() {
			@Override
			public void mouseDragged(PInputEvent event) {
				updateToolTip(event);
			}

			@Override
			public void mouseMoved(PInputEvent event) {
				updateToolTip(event);
			}

			public void updateToolTip(PInputEvent event) {
				PNode n = event.getInputManager().getMouseOver()
						.getPickedNode();
				String name = (String) n.getAttribute("name");
				Point2D p = event.getCanvasPosition();

				event.getPath().canvasToLocal(p, pcamera);
				tooltipNode.setText(name);
				tooltipNode.setOffset(p.getX() + 8, p.getY() - 8);

			}
		});

		histogramView.setPreferredSize(new Dimension(600, 150));

		List<String> categoryNames = new ArrayList<String>(
				spModel.categories.keySet());
		Collections.sort(categoryNames);
		int categoryWidth = 20;
		int x = categoryWidth;
		for (String name : categoryNames) {
			final Category category = spModel.categories.get(name);
			int h = (int) ((double) category.data.size()
					/ spModel.maxCategotySize
					* histogramView.getPreferredSize().height * 0.7);
			int y = histogramView.getPreferredSize().height - 30 - h;
			PNode categoryNode = PPath.createRectangle(x, y, categoryWidth, h);
			final PText categoryText = new PText(category.category);
			final PText categoryValue = new PText(category.data.size() + "");

			categoryNode.addAttribute("name", category.category);
			categoryNode.setPaint(getColorByCategory(category.category));
			categoryText.setOffset(
					x + categoryWidth / 2 - categoryText.getWidth() / 2, y + h);
			categoryText.setPickable(false);
			categoryValue.setScale(0.8);
			categoryValue.setOffset(
					x + categoryWidth / 2 - categoryValue.getWidth() / 2, y
							- categoryText.getHeight());
			categoryValue.setTextPaint(Color.gray);
			categoryValue.setPickable(false);

			categoryNode.addInputEventListener(new PBasicInputEventHandler() {
				@Override
				public void mouseEntered(PInputEvent event) {
					super.mouseEntered(event);
					((PText) event.getPickedNode().getChild(1))
							.setTextPaint(Color.black);
					java.awt.Cursor a = new java.awt.Cursor(
							java.awt.Cursor.HAND_CURSOR);
					event.pushCursor(a);
				}

				@Override
				public void mouseExited(PInputEvent event) {
					super.mouseExited(event);
					((PText) event.getPickedNode().getChild(1))
							.setTextPaint(Color.gray);
					event.popCursor();
				}

				@Override
				public void mousePressed(PInputEvent event) {
					category.toggleActivation();
					if (category.isActivated) {
						event.getPickedNode().setPaint(
								getColorByCategory(category.category));
					} else {
						event.getPickedNode().setPaint(Color.WHITE);
					}
					tableFilter.add(2, new RowFilter<Object, Object>() {
						@Override
						public boolean include(
								Entry<? extends Object, ? extends Object> entry) {
							String categoryName = ((Category) entry
									.getValue(entry.getValueCount() - 1)).category;
							if (spModel.categories.get(categoryName).isActivated)
								return true;
							else
								return false;
						}
					});
					((TableRowSorter<TableModel>) detailTable.getRowSorter())
							.setRowFilter(RowFilter.andFilter(tableFilter));
					try {
						filteringWorker.setTerminationRequested(true);
						t.join();
						t = new Thread(filteringWorker);
						t.start();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});

			histogramView.getLayer().addChild(categoryNode);
			categoryNode.addChild(categoryText);
			categoryNode.addChild(categoryValue);
			x += categoryWidth * 1.1;
		}
		return histogramView;
	}

	private TableRowSorter<TableModel> getRowSorter(JTable table) {
		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(
				table.getModel());
		sorter.setComparator(0, new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				// TODO Auto-generated method stub
				String i1 = (String) o1;
				String i2 = (String) o2;
				for (int i = 0; i < selectedItems.size(); i++) {
					if (selectedItems.get(i).compareTo(i1) == 0)
						return -1;
					if (selectedItems.get(i).compareTo(i2) == 0)
						return 1;
				}
				return 0;
			}
		});
		Vector<RowSorter.SortKey> sortkeys = new Vector<RowSorter.SortKey>();
		sortkeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
		sortkeys.add(new RowSorter.SortKey(3, SortOrder.ASCENDING));
		sorter.setSortKeys(sortkeys);
		return sorter;
	}

	private JPanel getSmallFilterPanel(JCheckBox scaleCheckBox) {
		JPanel smallSliderPanel = new JPanel();
		smallSliderPanel.setLayout(new BoxLayout(smallSliderPanel,
				BoxLayout.Y_AXIS));

		JPanel smallSubPanel = new JPanel();
		smallSliderPanel.add(smallSubPanel);

		JLabel smallFilterLabel = new JLabel("RPKM Filter");
		smallFilterLabel.setPreferredSize(new Dimension(100, 20));
		final IntegerTextField smallTextField = new IntegerTextField();
		smallSlider = new JSlider();
		if (isLogScale) {
			smallSlider.setMaximum((int) (Math.log(spModel.getMax() + .1)
					/ Math.log(2) * 1000));
			smallSlider.setMinimum((int) (Math.log(.1) / Math.log(2) * 1000));
			smallSlider.setValue(smallSlider.getMinimum());
		} else {
			smallSlider.setMaximum((int) (spModel.getMax() * 1000));
			smallSlider.setMinimum(0);
			smallSlider.setValue(smallSlider.getMinimum());
		}

		smallSubPanel.setLayout(new FlowLayout());
		smallSubPanel.add(smallFilterLabel);
		smallSubPanel.add(smallTextField);

		/***
		 * Small Text Field
		 */
		smallTextField.setPreferredSize(new Dimension(100, 20));
		smallTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		smallTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int key = e.getKeyCode();
				if (key == KeyEvent.VK_ENTER) {
					try {
						double v = Double.parseDouble(smallTextField.getText());
						smallSlider.setValue((int) (v * 1000));
					} catch (NumberFormatException e1) {
						return;
					}
				}
			}
		});
		smallTextField.setEditable(true);
		smallTextField.setFocusable(true);

		smallSliderPanel.add(smallSlider);
		smallSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				JSlider me = (JSlider) arg0.getSource();
				double prevSmall = smallFilter;
				smallFilter = me.getValue() / 1000.0 - .1;
				DoubleAndInt dummy = new DoubleAndInt();

				int insertionPoint1;
				int insertionPoint2;

				// if (isLogScale) {
				// dummy.d = new ArrayList<Double>();
				// for (int i = 0; i < spModel.getDataColumnNames().size(); i++)
				// {
				// dummy.d.add(Math.exp(prevSmall * Math.log(2)) - 0.1);
				// }
				// dummy.i = -1;
				// insertionPoint1 = -(Collections.binarySearch(
				// smallSortedDots, dummy, new SmallComp()) + 1);
				//
				// dummy.d = new ArrayList<Double>();
				// for (int i = 0; i < spModel.getDataColumnNames().size(); i++)
				// {
				// dummy.d.add(Math.exp(smallFilter * Math.log(2)) - 0.1);
				// }
				// insertionPoint2 = -(Collections.binarySearch(
				// smallSortedDots, dummy, new SmallComp()) + 1);
				// } else {
				// dummy.d = new ArrayList<Double>();
				// for (int i = 0; i < spModel.getDataColumnNames().size(); i++)
				// {
				// dummy.d.add(prevSmall);
				// }
				// dummy.i = -1;
				// insertionPoint1 = -(Collections.binarySearch(
				// smallSortedDots, dummy, new SmallComp()) + 1);
				//
				// dummy.d = new ArrayList<Double>();
				// for (int i = 0; i < spModel.getDataColumnNames().size(); i++)
				// {
				// dummy.d.add(smallFilter);
				// }
				//
				// insertionPoint2 = -(Collections.binarySearch(
				// smallSortedDots, dummy, new SmallComp()) + 1);
				//
				// }
				// if (insertionPoint1 < insertionPoint2) {
				// for (int i = insertionPoint1; i < insertionPoint2; i++) {
				// dots.get(smallSortedDots.get(i).i).setVisible(false);
				// dots.get(smallSortedDots.get(i).i)
				// .setPaintInvalid(true);
				// }
				// } else {
				// for (int i = insertionPoint2; i < insertionPoint1; i++) {
				// dots.get(smallSortedDots.get(i).i).setVisible(true);
				// dots.get(smallSortedDots.get(i).i).repaint();
				// }
				// }
				try {
					filteringWorker.setTerminationRequested(true);
					t.join();
					t = new Thread(filteringWorker);
					t.start();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				tableFilter.set(0, new RowFilter<Object, Object>() {
					@Override
					public boolean include(
							Entry<? extends Object, ? extends Object> entry) {
						int col1 = xIndex + 1;
						int col2 = yIndex + 1;
						// +1 은 이름 때문에
						if (isLogScale) {
							if (Math.log(Double.parseDouble(""
									+ entry.getValue(col1)) + .1)
									/ Math.log(2) > smallFilter
									&& Math.log(Double.parseDouble(""
											+ entry.getValue(col2)) + .1)
											/ Math.log(2) > smallFilter)
								return true;
							else
								return false;
						} else {
							if (Double.parseDouble("" + entry.getValue(col1)) > smallFilter
									&& Double.parseDouble(""
											+ entry.getValue(col2)) > smallFilter)
								return true;
							else
								return false;
						}
					}

				});
				((TableRowSorter<TableModel>) detailTable.getRowSorter())
						.setRowFilter(RowFilter.andFilter(tableFilter));
				smallTextField.setText("" + me.getValue() / 1000.0);
			}
		});

		smallSlider.setPaintLabels(true);
		smallSlider.setPaintTicks(true);

		final Hashtable<Integer, JLabel> labelTableLogScale = new Hashtable<Integer, JLabel>();
		for (int i = (int) log2(spModel.getMin() + 0.1); i < log2(spModel
				.getMax()); i++) {
			labelTableLogScale.put(new Integer(i * 1000), new JLabel(i + ""));
		}
		final Hashtable<Integer, JLabel> labelTableOriginalScale = new Hashtable<Integer, JLabel>();
		for (int i = 0; i < spModel.getMax(); i += Math.pow(10,
				(int) Math.log10(spModel.getMax()))) {
			labelTableOriginalScale.put(new Integer(i * 1000), new JLabel(i
					+ ""));
		}

		smallSlider.setLabelTable(labelTableLogScale);
		scaleCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				JCheckBox me = (JCheckBox) e.getSource();
				isLogScale = me.isSelected();
				if (isLogScale) {
					smallSlider.setValue((int) log2(smallSlider.getValue() / 1000.0) * 1000);
					smallSlider.setLabelTable(labelTableLogScale);
					smallSlider.setMaximum((int) (Math.log(spModel.getMax() + .1)
							/ Math.log(2) * 1000));
					smallSlider.setMinimum((int) (Math.log(.1) / Math.log(2) * 1000));
				} else {
					smallSlider.setValue((int) (exp2((double) smallSlider
							.getValue() / 1000) * 1000));
					smallSlider.setLabelTable(labelTableOriginalScale);
					smallSlider.setMaximum((int) (spModel.getMax() * 1000));
					smallSlider.setMinimum(0);
				}
			}
		});
		return smallSliderPanel;
	}

	void initFilters() {
		tableFilter.add(RowFilter.regexFilter(""));
		tableFilter.add(RowFilter.regexFilter(""));
		tableFilter.add(RowFilter.regexFilter(""));
		tableFilter.add(RowFilter.regexFilter(""));
	}

	private void initLayout() {
		makeMenubar();
		canvasPopup = getCanvasPopup();
		canvas.addInputEventListener(new PBasicInputEventHandler() {
			@Override
			public void mouseClicked(PInputEvent event) {
				if (event.getButton() == MouseEvent.BUTTON3) {
					canvasPopup.show((Component) event.getComponent(),
							(int) event.getCanvasPosition().getX(), (int) event
									.getCanvasPosition().getY());
				}
				super.mouseClicked(event);
			}

			@Override
			public void mouseWheelRotated(PInputEvent event) {

				if (event.getWheelRotation() > 0) {
					canvas.getCamera().scaleViewAboutPoint(0.8,
							event.getPosition().getX(),
							event.getPosition().getY());
				} else {
					canvas.getCamera().scaleViewAboutPoint(1 / 0.8,
							event.getPosition().getX(),
							event.getPosition().getY());
				}
				super.mouseWheelRotated(event);
			}
		});

		canvasPanel.setLayout(new BoxLayout(canvasPanel, BoxLayout.Y_AXIS));

		JPanel columnPanel = new JPanel();
		xColumnList = new JComboBox<String>(spModel.getDataColumnNames());
		xColumnList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				xIndex = xColumnList.getSelectedIndex();
				Dimension dim = canvas.getPreferredSize();
				xAxisLabel.setText((String) xColumnList.getSelectedItem());
				xAxisLabel.setOffset(dim.getWidth() - xAxisLabel.getWidth(),
						dim.getHeight());
				maxX = spModel.getMax(xIndex);
				resetDots();
				if (axisLayer != null) {
					axisLayer.repaint();
				}
			}
		});
		xColumnList.setSelectedIndex(xIndex);
		columnPanel.add(xColumnList);

		yColumnList = new JComboBox<String>(spModel.getDataColumnNames());
		yColumnList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				yIndex = yColumnList.getSelectedIndex();
				yAxisLabel.setText((String) yColumnList.getSelectedItem());
				yAxisLabel.setOffset(-yAxisLabel.getHeight(),
						yAxisLabel.getWidth());
				maxY = spModel.getMax(yIndex);
				resetDots();
				if (axisLayer != null) {
					axisLayer.repaint();
				}
			}
		});
		yColumnList.setSelectedIndex(yIndex);
		columnPanel.add(yColumnList);

		rightPanel = new JPanel();

		mainFrame.add(canvasPanel, BorderLayout.CENTER);
		mainFrame.add(rightPanel, BorderLayout.EAST);
		canvas.setPreferredSize(new java.awt.Dimension(600, 600));
		canvas.setZoomEventHandler(null);

		canvasPanel.add(canvas);
		canvasPanel.add(columnPanel);

		JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));

		statusLabel = new JLabel(" ");

		statusPanel.add(statusLabel);
		mainFrame.add(statusPanel, BorderLayout.SOUTH);

		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		TableModel detailTableModel = new NonEditableTableModel(
				spModel.getDataTable(), spModel.getColumnNames());

		detailTable = new JTable(detailTableModel);

		final JPopupMenu popup = new JPopupMenu();
		JMenuItem export = new JMenuItem("Export as ...");
		export.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				FileDialog fd = new FileDialog(new Frame(), "Save ...",
						FileDialog.SAVE);
				fd.setVisible(true);
				try {
					FileWriter writer = new FileWriter(fd.getDirectory()
							+ fd.getFile());
					for (int i = 0; i < detailTable.getRowCount(); i++) {
						for (int j = 0; j < detailTable.getColumnCount(); j++) {
							if (detailTable.getValueAt(i, j) instanceof Double) {
								writer.append(""
										+ (detailTable
												.getValueAt(i, j)));
							} else {
								writer.append(detailTable.getValueAt(i, j)
										.toString());
							}
							writer.append("\t");
						}
						writer.append("\r\n");// \n for linux
					}
					writer.flush();
					writer.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		popup.add(export);

		detailTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					JTable me = ((JTable) e.getSource());
					if (clickedIndex != -1) {
						PPath dot = dots.get(clickedIndex);
						Color c = (Color) dot.getAttribute("color");
						dot.setPaint(c);
						dot.setPathToEllipse(
								(float) (dot.getX() + dot.getWidth() / 2 - normalRadius),
								(float) (dot.getY() + dot.getHeight() / 2 - normalRadius),
								2 * normalRadius, 2 * normalRadius);

					}
					clickedIndex = me.convertRowIndexToModel(me
							.getSelectedRow());
					PPath dot = dots.get(clickedIndex);
					dot.setPaint(clickedColor);
					dot.setPathToEllipse(
							(float) (dot.getX() + dot.getWidth() / 2 - hoveredRadius),
							(float) (dot.getY() + dot.getHeight() / 2 - hoveredRadius),
							2 * hoveredRadius, 2 * hoveredRadius);
					dot.moveToFront();
					dotLayer.repaint();
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
		detailTable.setRowSorter(getRowSorter(detailTable));

		JScrollPane tablePane = new JScrollPane(detailTable);
		tablePane.setPreferredSize(new Dimension(600, 300));
		rightPanel.add(tablePane);

		scaleCheckBox = new JCheckBox("Log Scale");
		scaleCheckBox.setSelected(true);
		scaleCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JCheckBox me = (JCheckBox) e.getSource();
				isLogScale = me.isSelected();
				updateMinXY();
				resetDots();
				tickHandler.resetTicks();
				smallSlider.setValue(smallSlider.getValue());
			}
		});
		rightPanel.add(scaleCheckBox);

		JPanel smallFilterPanel = getSmallFilterPanel(scaleCheckBox);
		rightPanel.add(smallFilterPanel);

		JPanel equalPanel = getEqualFilterPanel();
		rightPanel.add(equalPanel);

		PCanvas histogramView = getHistogram();
		rightPanel.add(histogramView);
	}

	private double log2(double a) {
		return Math.log(a) / Math.log(2);
	}

	private void makeColorMap() {
		int alpha = 200;
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

	private void makeMenubar() {
		menuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
		JMenuItem open = new JMenuItem("Open", KeyEvent.VK_O);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fileDialog.show();
				File file = fileDialog.getFile();
				if (file != null) {
					xAxisLabel.setVisible(false);
					yAxisLabel.setVisible(false);
					xMaxLabel.setVisible(false);
					yMaxLabel.setVisible(false);
					spModel.readTXTData(file.getPath());
					fileChanged();
					xAxisLabel.setVisible(true);
					yAxisLabel.setVisible(true);
					xMaxLabel.setVisible(true);
					yMaxLabel.setVisible(true);
				}
			}

		});
		fileMenu.add(open);

		JMenuItem pngExport = new JMenuItem("Export as PNG");
		pngExport.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				FileDialog fd = new FileDialog(mainFrame);
				fd.setVisible(true);
				File screenShot = new File(fd.getDirectory() + fd.getFile());
				saveScreenImage(screenShot);
			}
		});
		fileMenu.add(pngExport);

		JMenuItem close = new JMenuItem("Exit", KeyEvent.VK_X);
		close.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				System.exit(0);
			}
		});
		fileMenu.add(close);

		menuBar.add(fileMenu);

		JMenu cameraMenu = new JMenu("Camera");
		JMenuItem resetCamera = new JMenuItem("Reset camera", KeyEvent.VK_R);
		resetCamera.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
				ActionEvent.ALT_MASK));
		resetCamera.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				PAffineTransform initialBound = (PAffineTransform) canvas
						.getCamera().getAttribute("initialTransform");
				canvas.getCamera().setViewTransform(initialBound);
			}
		});
		cameraMenu.add(resetCamera);

		menuBar.add(cameraMenu);

		JMenu optionMenu = new JMenu("Option");
		JMenuItem option = new JMenuItem("Option");
		option.addActionListener(tickHandler);
		optionMenu.add(option);
		menuBar.add(optionMenu);

		mainFrame.setJMenuBar(menuBar);
	}

	private void resetDots() {
		Dimension2D dim = canvas.getPreferredSize();
		if (dots != null) {
			for (int i = 0; i < dots.size(); i++) {
				ExpressionData data = spModel.getDataTable().get(i);
				double[] xy = getAdjustedLocation(data, isLogScale);
				dots.get(i)
						.setX((float) (xy[0] / (max - min) * dim.getWidth() - normalRadius));
				dots.get(i)
						.setY((float) (dim.getHeight()
								- (xy[1] / (max - min) * dim.getHeight()) - normalRadius));
				dots.get(i).setVisible(true);
			}
			dotLayer.repaint();
		}
	}

	private void saveScreenImage(File screenShot) {
		BufferedImage image = (BufferedImage) canvas.getCamera().toImage();
		try {
			ImageIO.write(image, "PNG", screenShot);
		} catch (IOException e2) {
			e2.printStackTrace();
		}

	}

	/*
	 * start함수내의 무한 루프가 빠지고 대신 <code>drawEverything</code>이란 함수에서 랜더링 작업을 수행한다.
	 */
	public void start() {
		updateMinXY();
		makeColorMap();
		initFilters();
		initLayout();
		drawEverything();

		mainFrame.pack();
		mainFrame.setVisible(true);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	private void updateMinXY() {
		if (isLogScale) {
			max = log2(spModel.getMax());
			min = log2(0.1);
			maxX = spModel.getMax(xIndex);
		} else {
			max = spModel.getMax();
			min = 0;
			maxY = spModel.getMax(yIndex);
		}
	}
}
