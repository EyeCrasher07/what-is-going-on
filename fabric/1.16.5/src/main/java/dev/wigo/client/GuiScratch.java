package dev.wigo.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Isolated fixed-function framebuffer; never changes Minecraft's Window or source GUI scale. */
public final class GuiScratch implements AutoCloseable {
    private static GuiScratch current;
    private final GuiScratch previous;
    private final LegacyEntityCamera entityCamera=new LegacyEntityCamera();
    private final RenderTarget target;
    private final int scale;
    private final LegacyGlState saved;
    private GuiScratch(RenderTarget target,int scale) {
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();saved=new LegacyGlState();
        this.target=target;this.scale=scale;previous=current;current=this;
        target.bindWrite(true);RenderSystem.disableScissor();RenderSystem.color4f(1,1,1,1);RenderSystem.disableFog();
        RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableAlphaTest();RenderSystem.alphaFunc(GL11.GL_GREATER,0.1f);
        RenderSystem.matrixMode(GL11.GL_PROJECTION);RenderSystem.loadIdentity();
        RenderSystem.ortho(0,target.width/(double)scale,target.height/(double)scale,0,1000,21000);
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);RenderSystem.loadIdentity();RenderSystem.translatef(0,0,-11000);
        Lighting.setupFor3DItems();
    }
    public static GuiScratch open(RenderTarget target,int scale) {return new GuiScratch(target,scale);}
    public static RenderTarget target() {return current==null?null:current.target;}
    public static int scale() {return current==null?0:current.scale;}
    public static RenderTarget create(int width,int height) {
        try(var state=new LegacyGlState()) {return new RenderTarget(width,height,true,Minecraft.ON_OSX);}
    }
    public static void destroy(RenderTarget target) {try(var state=new LegacyGlState()) {target.destroyBuffers();}}
    public static void clear(RenderTarget target) {
        target.setClearColor(0,0,0,0);RenderSystem.disableScissor();RenderSystem.colorMask(true,true,true,true);RenderSystem.depthMask(true);
        target.clear(Minecraft.ON_OSX);target.bindWrite(true);
    }
    public void close() {entityCamera.close();current=previous;saved.close();}
}
