package dev.wigo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.*;

/** Restore both actual OpenGL state and Minecraft's cached switches after a local GUI pass. */
final class LegacyGlState implements AutoCloseable {
    final int mode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    final int draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    final int[] viewport=new int[4],scissor=new int[4];
    final float[] color=new float[4];
    final boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),blend=GL11.glIsEnabled(GL11.GL_BLEND),alpha=GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
    final boolean texture=GL11.glIsEnabled(GL11.GL_TEXTURE_2D),fog=GL11.glIsEnabled(GL11.GL_FOG),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);
    final boolean scissoring=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),writeDepth=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    final boolean logic=GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP);
    final boolean lighting=GL11.glIsEnabled(GL11.GL_LIGHTING),material=GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL),rescale=GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
    final int logicOp=GL11.glGetInteger(GL11.GL_LOGIC_OP_MODE);
    final int textureId=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),shade=GL11.glGetInteger(GL11.GL_SHADE_MODEL),depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
    final int alphaFunc=GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
    final float alphaRef=GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
    final int src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    final int srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    LegacyGlState() {
        GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX,scissor);GL11.glGetFloatv(GL11.GL_CURRENT_COLOR,color);
        RenderSystem.matrixMode(GL11.GL_PROJECTION);RenderSystem.pushMatrix();
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);RenderSystem.pushMatrix();
        RenderSystem.pushLightingAttributes();
    }
    public void close() {
        RenderSystem.popAttributes();
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);RenderSystem.popMatrix();
        RenderSystem.matrixMode(GL11.GL_PROJECTION);RenderSystem.popMatrix();RenderSystem.matrixMode(mode);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);
        RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
        if(scissoring)RenderSystem.enableScissor(scissor[0],scissor[1],scissor[2],scissor[3]);else RenderSystem.disableScissor();
        if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();RenderSystem.depthMask(writeDepth);RenderSystem.depthFunc(depthFunc);
        if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();RenderSystem.blendFuncSeparate(src,dst,srcAlpha,dstAlpha);
        if(alpha)RenderSystem.enableAlphaTest();else RenderSystem.disableAlphaTest();RenderSystem.alphaFunc(alphaFunc,alphaRef);
        if(texture)RenderSystem.enableTexture();else RenderSystem.disableTexture();RenderSystem.bindTexture(textureId);
        if(fog)RenderSystem.enableFog();else RenderSystem.disableFog();
        if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
        // glPopAttrib restores the driver switches; update Minecraft's cached
        // values too, so the next native item pass can change them normally.
        if(lighting)RenderSystem.enableLighting();else RenderSystem.disableLighting();
        if(material)RenderSystem.enableColorMaterial();else RenderSystem.disableColorMaterial();
        if(rescale)RenderSystem.enableRescaleNormal();else RenderSystem.disableRescaleNormal();
        if(logic)RenderSystem.enableColorLogicOp();else RenderSystem.disableColorLogicOp();GlStateManager._logicOp(logicOp);
        RenderSystem.shadeModel(shade);RenderSystem.color4f(color[0],color[1],color[2],color[3]);
    }
}
