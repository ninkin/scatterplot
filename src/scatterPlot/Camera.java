package scatterPlot;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class Camera {

    private static final float MOUSE_SENSITIVITY = 1f;
    private static final float KEY_SENSITIVITY = 10000f;

    // Angle coordinates of the camera
    private float xAngle = 0f;
    private float yAngle = 0f;

    // Position coordinates of the camera
    private float xPos = 0f;
    private float yPos = 0f;
    private float zPos = 0f;

    public float zoomLevel = 1;
    private float zoomInFactor = 0.75f;
    private float zoomOutFactor = 1.33f;

    private boolean leftClick = false;
    private boolean rightClick = false;
    private float xrightCurrent = 0.0f;
    private float xrightPrevious = 0.0f;
    private float xrightDiff = 0.0f;
    private float yrightCurrent = 0.0f;
    private float yrightPrevious = 0.0f;
    private float yrightDiff = 0.0f;
    private float xleftCurrent = 0.0f;
    private float xleftPrevious = 0.0f;
    private float xleftDiff = 0.0f;
    private float yleftCurrent = 0.0f;
    private float yleftPrevious = 0.0f;
    private float yleftDiff = 0.0f;


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

    public void getInput(){

        // Catch user's mouse input
        getMouseInput();
        getKeyInput();
    }

    /**
	 * Get user input from mouse. This feedback from the user determines
     * the viewing angle. Some constrains are put here, the camera must not
     * be able to turn to any angle.
	 */
    private void getMouseInput(){

    	if(Mouse.isButtonDown(0) && leftClick){
    		xleftCurrent = Mouse.getX();
    		yleftCurrent = Mouse.getY();

    		xleftDiff = xleftCurrent - xleftPrevious;
    		yleftDiff = yleftCurrent - yleftPrevious;

    		xleftPrevious = xleftPrevious*MOUSE_SENSITIVITY;
    		yleftPrevious = yleftPrevious*MOUSE_SENSITIVITY;




    	}
    	else if(Mouse.isButtonDown(0) && !leftClick) {
            xleftPrevious = Mouse.getX();
            yleftPrevious = Mouse.getY();
            leftClick = true;
        }
    	else if(Mouse.isButtonDown(1) && rightClick) {

            xrightCurrent = Mouse.getX();
            yrightCurrent = Mouse.getY();

            xrightDiff = xrightCurrent - xrightPrevious;
            yrightDiff = yrightCurrent - yrightPrevious;

            xrightPrevious = xrightCurrent;
            yrightPrevious = yrightCurrent;

            xAngle = xAngle + xrightDiff*MOUSE_SENSITIVITY;

            if (xAngle > 360.0f)
                xAngle = xAngle - 360.0f;
            else if (xAngle < -360.0f)
                xAngle = xAngle + 360.0f;

            // Vertical movement of the mouse
            yAngle = yAngle + yrightDiff*MOUSE_SENSITIVITY;

            float minAngle;
            minAngle = 180.0f;

            // Don't go below floor
            yAngle = Math.min(yAngle, minAngle);

            // Don't go up and opposite
            yAngle = Math.max(yAngle, 0.0f);

            rightClick = true;
        }
        else if(Mouse.isButtonDown(1) && !rightClick) {
            xrightPrevious = Mouse.getX();
            yrightPrevious = Mouse.getY();
            rightClick = true;
        }
        else{
            rightClick = false;
            leftClick = false;

        }

        // Check if wheel has been scrolled
        int wheelMovement = Mouse.getDWheel();
        // If scrolled up
        if (wheelMovement > 0){
        	zoomLevel *= zoomInFactor;
        }
        // If scrolled down
        if (wheelMovement < 0){
        	zoomLevel *= zoomOutFactor;
        }

    }
    private void getKeyInput(){

        // If direction buttons are pressed
        if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
        	if(rotateToOrigin){
        		yPos += KEY_SENSITIVITY * zoomLevel;
        	}
        	else{
	            xPos += (float)(Math.sin(Math.toRadians(xAngle))*KEY_SENSITIVITY*zoomLevel);
	            yPos += (float)(Math.cos(Math.toRadians(xAngle))*KEY_SENSITIVITY*zoomLevel);
	            zPos -= (float)(Math.sin(Math.toRadians(yAngle))*KEY_SENSITIVITY*zoomLevel);
        	}
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
        	if(rotateToOrigin){
        		yPos -= KEY_SENSITIVITY * zoomLevel;
        	}
        	else{
	            xPos -= (float)(Math.sin(Math.toRadians(xAngle))*KEY_SENSITIVITY*zoomLevel);
	            yPos -= (float)(Math.cos(Math.toRadians(xAngle))*KEY_SENSITIVITY*zoomLevel);
	            zPos += (float)(Math.sin(Math.toRadians(yAngle))*KEY_SENSITIVITY*zoomLevel);
        	}
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
        	if(rotateToOrigin){
        		xPos += KEY_SENSITIVITY * zoomLevel;
        	}
        	else{
	            xPos += (float)(Math.sin(Math.toRadians(90.0f-xAngle))*KEY_SENSITIVITY*zoomLevel);
	            yPos -= (float)(Math.cos(Math.toRadians(90.0f-xAngle))*KEY_SENSITIVITY*zoomLevel);
        	}
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
        	if(rotateToOrigin){
        		xPos -= KEY_SENSITIVITY * zoomLevel;
        	}
        	else{
	            xPos -= (float)(Math.sin(Math.toRadians(90.0f-xAngle))*KEY_SENSITIVITY*zoomLevel);
	            yPos += (float)(Math.cos(Math.toRadians(90.0f-xAngle))*KEY_SENSITIVITY*zoomLevel);
        	}
        }

    }
    public void setView(String mode){
    	if(mode.compareTo("XY")==0){
    		xPos = 0;
    		yPos = 0;
    		zPos = 0;
    		xAngle = 0;
    		yAngle = 0;
    	}
    	else if(mode.compareTo("YZ")==0){
    		xPos = 0;
    		yPos = 0;
    		zPos = 0;
    		xAngle = 90;
    		yAngle = 90;

    	}
    	else if(mode.compareTo("XZ")==0){
    		xPos = 0;
    		yPos = 0;
    		zPos = 0;
    		xAngle = 0;
    		yAngle = 90;
    	}
    }

    /**
	 * Move the camera. Place the camera according to the user's zoom level,
     * angle and position.
	 */
    public void move(){
    	zoom();

        moveFree();
    }
    private void zoom(){
    	GL11.glOrtho(left_plane * zoomLevel, right_plane * zoomLevel, bottom_plane * zoomLevel, top_plane * zoomLevel, near_plane, far_plane);
    }
    /**
	 * Move the camera in free mode
	 */
    private void moveFree(){
    	GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();

        // Check for room boundaries

		if(rotateToOrigin){
	        GL11.glTranslatef(-xPos, -yPos, zPos);
	        GL11.glRotatef(-yAngle, 1.0f, 0.0f, 0.0f);
	        GL11.glRotatef(xAngle, 0.0f, 0.0f, 1.0f);
		}
		else{
	        GL11.glRotatef(-yAngle, 1.0f, 0.0f, 0.0f);
	        GL11.glRotatef(xAngle, 0.0f, 0.0f, 1.0f);
	        GL11.glTranslatef(-xPos, -yPos, zPos);
		}
    }
}