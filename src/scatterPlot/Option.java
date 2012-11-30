package scatterPlot;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Option extends JDialog {
	public boolean showTick = false;
	public double tickInterval = 1000;
	public boolean isOK = false;
	Option me;

	public Option(Frame owner, String title, boolean modal) {
		super(owner, title, modal);
		me = this;
		JPanel panel = new JPanel();

		final JCheckBox showTickCheck = new JCheckBox("Show Tick on the Axises");
		showTickCheck.setSelected(showTick);
		panel.add(showTickCheck);

		final IntegerTextField intervalField = new IntegerTextField();
		intervalField.setHorizontalAlignment(JTextField.RIGHT);
		intervalField.setText(tickInterval + "");
		intervalField.setPreferredSize(new Dimension(100, 20));

		panel.add(intervalField);

		JButton submit = new JButton("Confirm");
		submit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				showTick = showTickCheck.isSelected();
				tickInterval = Double.parseDouble(intervalField.getText());
				isOK = true;
				me.dispose();
			}
		});
		panel.add(submit);

		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				me.dispose();
			}
		});
		panel.add(cancel);

		me.add(panel);
		me.pack();
	}
}
