package dev.wigo.client;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.Lighting;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;

/** Scoped output/projection; never changes Minecraft's window or main framebuffer. */
public final class GuiScratch implements AutoCloseable {
    private static GuiScratch current;
    private final GuiScratch previous;
    private final LegacyEntityCamera entityCamera=new LegacyEntityCamera();
    private final RenderTarget target;
    private final int scale;
    private final Matrix4f projection=new Matrix4f(RenderSystem.getProjectionMatrix());
    private final ProjectionType projectionType=RenderSystem.getProjectionType();
    private GuiScratch(RenderTarget target,int scale) {
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        this.target=target;this.scale=scale;previous=current;current=this;
        RenderSystem.getModelViewStack().pushMatrix().translation(0,0,-11000);
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,target.width/(float)scale,target.height/(float)scale,0,1000,21000),ProjectionType.ORTHOGRAPHIC);
        Lighting.setupFor3DItems();
    }
    public static GuiScratch open(RenderTarget target,int scale) {return new GuiScratch(target,scale);}
    public static RenderTarget target() {return current==null?null:current.target;}
    public static int scale() {return current==null?0:current.scale;}
    @Override public void close() {
        entityCamera.close();
        RenderSystem.disableScissor();RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.setProjectionMatrix(projection,projectionType);current=previous;
    }
}
