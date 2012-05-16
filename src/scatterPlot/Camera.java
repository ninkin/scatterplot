package scatterPlot;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class Camera {



	private boolean leftClick = false;
	private int []leftPrevious;


	public boolean rotateToOrigin = true;


	//setting viewing volumne
	private double left_plane;
	private double right_plane;
	private double bottom_plane;
	private double top_plane;
	private double near_plane;
	private double far_plane;


	public Camera(double left_plane, double right_plane, double bottom_plane, double top_plane, double near_plane, double far_plane){
		this.left_plane = left_plane;
		this.right_plane = right_plane;
		this.bottom_plane = bottom_plane;
		this.top_plane = top_plane;
		this.near_plane = near_plane;
		this.far_plane = far_plane;
	}
	public void setCamera(double left_plane, double right_plane, double bottom_plane, double top_plane, double near_plane, double far_plane){
		this.left_plane = left_plane;
		this.right_plane = right_plane;
		this.bottom_plane = bottom_plane;
		this.top_plane = top_plane;
		this.near_plane = near_plane;
		this.far_plane = far_plane;
	}

	public void getInput(){

		// Catch user's mouse input
		getMouseInput();
	}

	private void getMouseInput(){
		if(Mouse.isButtonDown(0) && leftClick){
			int []leftCurrent = new int[]{Mouse.getX(), Mouse.getY()};
			float []preObj = Translater.getObjCoordinate(leftPrevious[0], leftPrevious[1]);
			float []curObj = Translater.getObjCoordinate(leftCurrent[0], leftCurrent[1]);


			left_plane += preObj[0] - curObj[0];
			right_plane += preObj[0] - curObj[0];

			top_plane += preObj[1] - curObj[1];
			bottom_plane += preObj[1] - curObj[1];


			leftPrevious = leftCurrent;
		}
		else if(Mouse.isButtonDown(0) && !leftClick) {
			leftPrevious = new int[]{Mouse.getX(), Mouse.getY()};
			leftClick = true;
		}
		else{
			leftClick = false;
		}

		// Check if wheel has been scrolled
		int wheelMovement = Mouse.getDWheel();
		if(wheelMovement != 0){
			float[] objXY = Translater.getObjCoordinate(Mouse.getX(), Mouse.getY());

			//If scrolled up
			if (wheelMovement > 0){
				double plane_width = right_plane-left_plane;
				double plane_height = top_plane-bottom_plane;
				double t = (top_plane - objXY[1])/plane_height;
				double b = (objXY[1] - bottom_plane)/plane_height;
				double l = (objXY[0] - left_plane)/plane_width;
				double r = (right_plane - objXY[0])/plane_width;

				left_plane = objXY[0]-plane_width*l/1.25;
				right_plane = objXY[0]+plane_width*r/1.25;
				top_plane = objXY[1]+plane_height*t/1.25;
				bottom_plane = objXY[1]-plane_height*b/1.25;
			}
			// If scrolled down
			if (wheelMovement < 0){
				double plane_width = right_plane-left_plane;
				double plane_height = top_plane-bottom_plane;
				double t = (top_plane - objXY[1])/plane_height;
				double b = (objXY[1] - bottom_plane)/plane_height;
				double l = (objXY[0] - left_plane)/plane_width;
				double r = (right_plane - objXY[0])/plane_width;

				left_plane = objXY[0]-plane_width*l/0.8;
				right_plane = objXY[0]+plane_width*r/0.8;
				top_plane = objXY[1]+plane_height*t/0.8;
				bottom_plane = objXY[1]-plane_height*b/0.8;
			}
		}

	}
	public void move(){
		GL11.glOrtho(left_plane, right_plane, bottom_plane, top_plane, near_plane, far_plane);
	}
}
