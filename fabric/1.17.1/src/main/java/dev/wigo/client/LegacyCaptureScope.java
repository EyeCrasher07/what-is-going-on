package dev.wigo.client;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.ShaderInstance;

/** Restore old screens' global projection, viewport and shader after describing them. */
public final class LegacyCaptureScope implements AutoCloseable {
    private final Matrix4f projection=new Matrix4f(RenderSystem.getProjectionMatrix());
    private final ShaderInstance shader=RenderSystem.getShader();
    private final float[] color=RenderSystem.getShaderColor().clone();
    private final int texture=RenderSystem.getShaderTexture(0);
    private final net.minecraft.resources.ResourceLocation captureTexture=GuiGraphics.captureTexture;
    private final boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),blend=GL11.glIsEnabled(GL11.GL_BLEND),writeDepth=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final int blendSrc=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),blendDst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),blendSrcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),blendDstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int[] viewport=new int[4];
    public LegacyCaptureScope() {GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);RenderSystem.getModelViewStack().pushPose();}
    public void close() {
        RenderSystem.getModelViewStack().popPose();RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(projection);RenderSystem.setShader(()->shader);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
        RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
        RenderSystem.setShaderTexture(0,texture);GuiGraphics.captureTexture=captureTexture;
        if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();RenderSystem.depthMask(writeDepth);
        if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();GlStateManager._blendFuncSeparate(blendSrc,blendDst,blendSrcAlpha,blendDstAlpha);
    }
}
