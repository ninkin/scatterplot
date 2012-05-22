package scatterPlot;

import java.awt.event.KeyEvent;

import javax.swing.JTextField;

public class IntegerTextField extends JTextField {
		final static String badchars
		= "`~!@#$%^&*()[]{}_+=\\|\"':;?/>.<, ";
		public void processKeyEvent(KeyEvent ev) {
			char c = ev.getKeyChar();
			if((Character.isLetter(c) && !ev.isAltDown())
					|| badchars.indexOf(c) > -1) {
				ev.consume();
				return;
			}
			if(c == '-' && getDocument().getLength() > 0)
				ev.consume();
			else super.processKeyEvent(ev);
		}
	}