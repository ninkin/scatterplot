package scatterPlot;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public class Translater {
	public static int []getViewport(){
		IntBuffer viewport = BufferUtils.createIntBuffer(16);
		GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
		return new int[]{viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3)};
	}
	public static int []getScreenCoordinate(float x, float y, float z){
		IntBuffer viewport = BufferUtils.createIntBuffer(16);
		GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

		FloatBuffer modelMatrix = BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelMatrix);

		FloatBuffer projMatrix = BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projMatrix);


		FloatBuffer win_pos = BufferUtils.createFloatBuffer(16);
		GLU.gluProject(x, y, z, modelMatrix, projMatrix, viewport, win_pos);

		return new int[]{(int)win_pos.get(0), (int)win_pos.get(1)};
	}

	public static float [] getObjCoordinate(int x, int y){
		IntBuffer viewport = BufferUtils.createIntBuffer(16);
		GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

		FloatBuffer modelMatrix = BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelMatrix);

		FloatBuffer projMatrix = BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projMatrix);


		FloatBuffer obj_pos = BufferUtils.createFloatBuffer(16);
		GLU.gluUnProject(x, y, 0, modelMatrix, projMatrix, viewport, obj_pos);

		return new float[]{obj_pos.get(0), obj_pos.get(1), obj_pos.get(2)};

	}
}
