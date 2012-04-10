package scatterPlot;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class Camera {

    private static final float MOUSE_SENSITIVITY = 0.5f;
    private static final float ZOOM_SENSITIVITY = 10f;
    private static final float KEY_SENSITIVITY = 1000f;
    private static final float WHEEL_SENSITIVITY = 3.0f;
    private static final float MIN_ANGLE = 80.0f; //180
    private static final float MAX_ZOOM_IN = 1f;
    private static final float MAX_ZOOM_OUT = Float.MAX_VALUE;// 26.0f;
    private static final float ZOOM_OFFSET = 1f;
    private static final float ZOOM_OFFSET_SPEED = 0.1f;

    // Angle coordinates of the camera
    private float xAngle = 0f;
    private float yAngle = 0f;

    // Position coordinates of the camera
    private float xPos = 0f;
    private float yPos = 0f;
    private float zPos = 0f;

    private float zoom = 1.0f;


    private boolean zoomIn;
    private boolean zoomOut;
    private boolean zoomInSmooth;
    private boolean zoomOutSmooth;
    private float finalZoom;

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

    private double zoomMax;

    private boolean freeMode = true;

    public Camera(double zoomMax){
    	this.zoomMax = zoomMax;
    }

    public void getInput(){

        // Catch user's mouse input
        getMouseInput();
        getKeyInput();
        // Provide a smooth effect
        smoothZoom();

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
            if (freeMode)
                minAngle = 180.0f;
            else
                minAngle = MIN_ANGLE;

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
            zoomIn = true;
            zoomOutSmooth = false;
            zoom += ZOOM_SENSITIVITY * WHEEL_SENSITIVITY;
        }
        else if (zoomIn){
            finalZoom = zoom - ZOOM_OFFSET;
            zoomInSmooth = true;
            zoomIn = false;
        }

        // If scrolled down
        if (wheelMovement < 0){
            zoomOut = true;
            zoomInSmooth = false;
            zoom -= ZOOM_SENSITIVITY * WHEEL_SENSITIVITY;
        }
        else if (zoomOut){
            finalZoom = zoom + ZOOM_OFFSET;
            zoomOutSmooth = true;
            zoomOut = false;
        }

    }
    private void getKeyInput(){

        // If PgUp is pressed
        if (Keyboard.isKeyDown(Keyboard.KEY_PRIOR)) {
            zoomIn = true;
            zoomOutSmooth = false;
            zoom -= ZOOM_SENSITIVITY;
        }
        else if (zoomIn){
            finalZoom = zoom - ZOOM_OFFSET;
            zoomInSmooth = true;
            zoomIn = false;
        }

        // If PgDn is pressed
        if (Keyboard.isKeyDown(Keyboard.KEY_NEXT)) {
            zoomOut = true;
            zoomInSmooth = false;
            zoom += ZOOM_SENSITIVITY;
        }
        else if (zoomOut){
            finalZoom = zoom + ZOOM_OFFSET;
            zoomOutSmooth = true;
            zoomOut = false;
        }

        // If direction buttons are pressed
        if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
            xPos += (float)(Math.sin(Math.toRadians(xAngle))*KEY_SENSITIVITY);
            yPos += (float)(Math.cos(Math.toRadians(xAngle))*KEY_SENSITIVITY);
            zPos += (float)(Math.signum(Math.cos(Math.toRadians(yAngle)))*KEY_SENSITIVITY);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
            xPos -= (float)(Math.sin(Math.toRadians(xAngle))*KEY_SENSITIVITY);
            yPos -= (float)(Math.cos(Math.toRadians(xAngle))*KEY_SENSITIVITY);
            zPos -= (float)(Math.signum(Math.cos(Math.toRadians(yAngle)))*KEY_SENSITIVITY);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
            xPos += (float)(Math.sin(Math.toRadians(90.0f-xAngle))*KEY_SENSITIVITY);
            yPos -= (float)(Math.cos(Math.toRadians(90.0f-xAngle))*KEY_SENSITIVITY);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
            xPos -= (float)(Math.sin(Math.toRadians(90.0f-xAngle))*KEY_SENSITIVITY);
            yPos += (float)(Math.cos(Math.toRadians(90.0f-xAngle))*KEY_SENSITIVITY);
        }

    }
    /**
	 * Make a smoothing effect when zooming. The final zoom level will be
     * extended a little bit more
	 */
    private void smoothZoom(){
        // If a zoom smoothing effect is required
        if (zoomInSmooth || zoomOutSmooth){
            if (finalZoom > MAX_ZOOM_IN || finalZoom < MAX_ZOOM_OUT){
                if (zoomInSmooth){
                    // If the final zoom level hasn't been reached yet
                    if (finalZoom + 0.0001 < zoom)
                        zoom -= ( (zoom - finalZoom) * ZOOM_OFFSET_SPEED);
                    else
                        zoomInSmooth = false;
                }
                else if (zoomOutSmooth){
                    // If the final zoom level hasn't been reached yet
                    if (finalZoom - 0.0001 > zoom)
                        zoom += ( (finalZoom - zoom) * ZOOM_OFFSET_SPEED);
                    else
                        zoomOutSmooth = false;
                }
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
    		zoom = 1f;
    	}
    	else if(mode.compareTo("YZ")==0){
    		xPos = 0;
    		yPos = 0;
    		zPos = 0;
    		xAngle = 90;
    		yAngle = 90;
    		zoom = 1f;

    	}
    	else if(mode.compareTo("XZ")==0){
    		xPos = 0;
    		yPos = 0;
    		zPos = 0;
    		xAngle = 0;
    		yAngle = 90;
    		zoom = 1f;
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
    	GL11.glOrtho(-zoomMax, zoomMax,
				-zoomMax, zoomMax, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
	 * Move the camera in free mode
	 */
    private void moveFree(){
    	GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		zPos = Math.min(0, zPos);

        zoom = Math.max(zoom, MAX_ZOOM_IN);
        zoom = Math.min(zoom, MAX_ZOOM_OUT);

        // Check for room boundaries

        GL11.glRotatef(-yAngle, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(xAngle, 0.0f, 0.0f, 1.0f);
        GL11.glTranslatef(-xPos, -yPos, zPos);
        GL11.glScalef(zoom, zoom, zoom);
    }

    /**
	 * Change the camera mode
	 */
    public void changeMode(){
        freeMode = !freeMode;
    }
}