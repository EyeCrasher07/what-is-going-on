package dev.wigo.client;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import java.util.ArrayDeque;

/** Track fixed-function transforms in memory; read GPU state once, never per UI instruction. */
public final class LegacyGlMatrices {
    private static final ArrayDeque<Matrix4f> STACK=new ArrayDeque<>();
    private static final float[] COLOR=new float[4];
    private static int mode;
    private static boolean textured;
    public static void begin() {
        var buffer=BufferUtils.createFloatBuffer(16);GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX,buffer);
        STACK.clear();STACK.push(new Matrix4f().set(buffer));mode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glGetFloatv(GL11.GL_CURRENT_COLOR,COLOR);
        textured=GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
    }
    public static void end() {STACK.clear();}
    private static boolean active() {return !STACK.isEmpty()&&mode==GL11.GL_MODELVIEW;}
    public static void mode(int value) {mode=value;}
    public static void texture(boolean value) {if(!STACK.isEmpty())textured=value;}
    public static boolean textured() {return textured;}
    public static void color(float r,float g,float b,float a) {
        if(!STACK.isEmpty()) {COLOR[0]=r;COLOR[1]=g;COLOR[2]=b;COLOR[3]=a;}
    }
    public static int color() {
        return (component(COLOR[3])<<24)|(component(COLOR[0])<<16)|(component(COLOR[1])<<8)|component(COLOR[2]);
    }
    private static int component(float value) {return Math.max(0,Math.min(255,Math.round(value*255)));}
    public static void identity() {if(active())STACK.peek().identity();}
    public static void push() {if(active())STACK.push(new Matrix4f(STACK.peek()));}
    public static void pop() {if(active()&&STACK.size()>1)STACK.pop();}
    public static void translate(float x,float y,float z) {if(active())STACK.peek().translate(x,y,z);}
    public static void scale(float x,float y,float z) {if(active())STACK.peek().scale(x,y,z);}
    public static void rotate(float angle,float x,float y,float z) {if(active())STACK.peek().rotate((float)Math.toRadians(angle),x,y,z);}
    public static void multiply(net.minecraft.util.math.vector.Matrix4f matrix) {if(active())STACK.peek().mul(LegacyMatrices.joml(matrix));}
    public static Matrix4f combine(net.minecraft.util.math.vector.Matrix4f local) {
        var result=STACK.isEmpty()?new Matrix4f():new Matrix4f(STACK.peek());
        return result.mul(LegacyMatrices.joml(local));
    }
}
