package scatterPlot;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class FileOpenDialog {

	/**
	 * @param args
	 */

	public FileOpenDialog(){
		final JFrame openWindow = new JFrame();

		openWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		openWindow.setVisible(true);
		openWindow.pack();

		final JFileChooser fc = new JFileChooser();

		int returnVal = fc.showOpenDialog(openWindow);

		if(returnVal == JFileChooser.APPROVE_OPTION){
			File file = fc.getSelectedFile();
		}
		else{
			openWindow.dispose();
		}
	}
}
