package scatterPlot;
import java.awt.Color;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

/**
 * 마우스 오버시에 보여주는 툴팁.
 * 
 * @author ninkin
 * 
 */
public class HTooltipNode extends PNode {
	/**
	 * text to show in the box.
	 */
	private PText tooltiptext = new PText();
	/**
	 * text box containing text.
	 */
	private PPath tooltipbox = new PPath();;

	private int innerPadding = 10;
	private int textOffset = 5;

	/**
	 * default constructor.
	 */
	public HTooltipNode() {
		tooltipbox.setPathToRectangle(0, 0, 0, 0);
		tooltipbox.setPaint(Color.yellow);
		this.addChild(tooltipbox);
		this.addChild(tooltiptext);
	}

	
	/**
	 * set tooltip text with certain string.
	 * 
	 * @param str
	 *            text
	 */
	public final void setText(final String str) {
		tooltiptext.setText(str);
		tooltiptext.setOffset(textOffset, 0);
		tooltipbox.setPathToRectangle(0, 0, (float) tooltiptext.getWidth()
				+ innerPadding, (float) tooltiptext.getHeight());
		this.setBounds(0, 0, (float) tooltiptext.getWidth() + innerPadding,
				(float) tooltiptext.getHeight());
	}
}
