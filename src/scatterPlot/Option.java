package scatterPlot;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class Option {
	static boolean showTick = false;
	static double tickInterval = 1000;
	public Option(){
		final JFrame optionFrame = new JFrame();
		JPanel panel = new JPanel();
		optionFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);



		final JCheckBox showTickCheck = new JCheckBox("Show Tick on the Axises");
		showTickCheck.setSelected(showTick);
		panel.add(showTickCheck);

		final IntegerTextField intervalField = new IntegerTextField();
		intervalField.setHorizontalAlignment(JTextField.RIGHT);
		intervalField.setText(tickInterval+"");
		intervalField.setPreferredSize(new Dimension(100, 20));

		panel.add(intervalField);

		JButton submit = new JButton("Confirm");
		submit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				showTick = showTickCheck.isSelected();
				tickInterval = Double.parseDouble(intervalField.getText());
				optionFrame.dispose();
			}
		});
		panel.add(submit);

		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				optionFrame.dispose();
			}
		});
		panel.add(cancel);

		optionFrame.add(panel);
		optionFrame.setVisible(true);
		optionFrame.pack();
	}
}
