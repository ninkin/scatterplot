package scatterPlot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import de.matthiasmann.twl.BoxLayout;

public class FileOpenDialog{

	/**
	 * @param args
	 */

	File file;
	public FileOpenDialog(){

	}

	public void show(){
		final JFrame openWindow = new JFrame();

		openWindow.setPreferredSize(new Dimension(400, 150));
		openWindow.setLayout(new FlowLayout());

		JPanel readOptions = new JPanel();

		openWindow.add(readOptions);

		JLabel biggerThanLabel = new JLabel("Read data bigger than ");
		IntegerTextField biggerThan = new IntegerTextField();

		biggerThan.setText("0.01");
		biggerThan.setPreferredSize(new Dimension(100, 20));
		biggerThan.setHorizontalAlignment(JTextField.RIGHT);

		readOptions.add(biggerThanLabel);
		readOptions.add(biggerThan);

		JPanel buttonsPanel = new JPanel();

		openWindow.add(buttonsPanel);

		JButton confirmButton = new JButton("Confirm");
		JButton cancelButton = new JButton("Cancel");

		confirmButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				openWindow.dispose();
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				file = null;
				openWindow.dispose();
			}
		});

		buttonsPanel.add(confirmButton);
		buttonsPanel.add(cancelButton);

//		openWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//		openWindow.setVisible(true);
//		openWindow.pack();

		final JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new TextFilter());

		int returnVal = fc.showOpenDialog(openWindow);

		if(returnVal == JFileChooser.APPROVE_OPTION){
			this.file = fc.getSelectedFile();
		}
		else{
			file = null;
			openWindow.dispose();
		}

	}
	public File getFile(){
		return file;
	}
	class TextFilter extends FileFilter{

		@Override
		public boolean accept(File f) {
			// TODO Auto-generated method stub
			if(f.isDirectory()){
				return true;
			}
			String extension = getExtension(f);
			if(extension != null){
				if(extension.equals("txt")){
					return true;
				}
				else{
					return false;
				}
			}
			return false;

		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return "Tap delimited text(*.txt)";
		}
	    public String getExtension(File f) {
	        String ext = null;
	        String s = f.getName();
	        int i = s.lastIndexOf('.');

	        if (i > 0 &&  i < s.length() - 1) {
	            ext = s.substring(i+1).toLowerCase();
	        }
	        return ext;
	    }
	}
}
