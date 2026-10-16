package dev.wigo.client;

import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;

/** Own framebuffer/viewport/projection only; Minecraft's Window is never modified. */
public final class GuiScratch implements AutoCloseable {
    private static GuiScratch current;
    private final GuiScratch previous;
    private final LegacyEntityCamera entityCamera=new LegacyEntityCamera();
    private final RenderTarget target;
    private final int scale;
    private final Matrix4f projection;
    private final VertexSorting projectionType;
    private final ShaderInstance shader;
    private final float[] color;
    private final GlTarget saved;
    private GuiScratch(RenderTarget target,int scale) {
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        saved=new GlTarget();projection=new Matrix4f(RenderSystem.getProjectionMatrix());
        projectionType=RenderSystem.getVertexSorting();shader=RenderSystem.getShader();color=RenderSystem.getShaderColor().clone();
        this.target=target;this.scale=scale;previous=current;current=this;
        target.bindWrite(true);RenderSystem.disableScissor();RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.getModelViewStack().pushMatrix().translation(0,0,-11000);RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,target.width/(float)scale,target.height/(float)scale,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
        Lighting.setupFor3DItems();
    }
    public static GuiScratch open(RenderTarget target,int scale) {return new GuiScratch(target,scale);}
    public static RenderTarget target() {return current==null?null:current.target;}
    public static int scale() {return current==null?0:current.scale;}
    public static TextureTarget create(int width,int height) {
        try(GlTarget saved=new GlTarget()) {return new TextureTarget(width,height,true,Minecraft.ON_OSX);}
    }
    public static void destroy(TextureTarget target) {
        try(GlTarget saved=new GlTarget()) {target.destroyBuffers();}
    }
    public static void clear(RenderTarget target) {
        target.setClearColor(0,0,0,0);RenderSystem.disableScissor();RenderSystem.colorMask(true,true,true,true);RenderSystem.depthMask(true);
        target.clear(Minecraft.ON_OSX);target.bindWrite(true);
    }
    @Override public void close() {
        entityCamera.close();
        RenderSystem.getModelViewStack().popMatrix();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,projectionType);
        RenderSystem.setShader(()->shader);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
        current=previous;saved.close();
    }
    private static final class GlTarget implements AutoCloseable {
        final int draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        final int[] viewport=new int[4],scissor=new int[4];
        final boolean scissoring=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),writeDepth=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        GlTarget() {GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX,scissor);}
        @Override public void close() {
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            if(scissoring)RenderSystem.enableScissor(scissor[0],scissor[1],scissor[2],scissor[3]);else RenderSystem.disableScissor();
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();RenderSystem.depthMask(writeDepth);
        }
    }
}
