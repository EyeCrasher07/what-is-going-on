package dev.wigo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import dev.wigo.mixin.RenderTypeInvoker;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/** Fixed-function brightness keeps opacity independent, including values above 100 percent. */
public final class LegacyPanelType extends RenderState {
    private static int previousMode,previousScale;
    public static int color(int alpha,int brightness) {
        float multiplier=brightness/(brightness>100?200f:100f);
        int rgb=Math.round(alpha*multiplier);
        return alpha<<24|rgb<<16|rgb<<8|rgb;
    }
    private static void tint() {
        previousMode=GL11.glGetTexEnvi(GL11.GL_TEXTURE_ENV,GL11.GL_TEXTURE_ENV_MODE);
        previousScale=GL11.glGetTexEnvi(GL11.GL_TEXTURE_ENV,GL13.GL_RGB_SCALE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL11.GL_TEXTURE_ENV_MODE,GL13.GL_COMBINE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL13.GL_COMBINE_RGB,GL11.GL_MODULATE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL13.GL_SOURCE0_RGB,GL11.GL_TEXTURE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL13.GL_SOURCE1_RGB,GL13.GL_PRIMARY_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL13.GL_OPERAND0_RGB,GL11.GL_SRC_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL13.GL_OPERAND1_RGB,GL11.GL_SRC_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL13.GL_COMBINE_ALPHA,GL11.GL_MODULATE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL13.GL_SOURCE0_ALPHA,GL11.GL_TEXTURE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL13.GL_SOURCE1_ALPHA,GL13.GL_PRIMARY_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL13.GL_OPERAND0_ALPHA,GL11.GL_SRC_ALPHA);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL13.GL_OPERAND1_ALPHA,GL11.GL_SRC_ALPHA);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL13.GL_RGB_SCALE,!Settings.autoBrightness&&Settings.brightness>100?2:1);
    }
    private static void restoreTint() {
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL13.GL_RGB_SCALE,previousScale);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,GL11.GL_TEXTURE_ENV_MODE,previousMode);
    }
    private static final net.minecraft.client.renderer.RenderState.TransparencyState OPAQUE=new net.minecraft.client.renderer.RenderState.TransparencyState("wigo_opaque",()->{
        tint();RenderSystem.disableBlend();
    },LegacyPanelType::restoreTint);
    private static final net.minecraft.client.renderer.RenderState.TransparencyState PREMULTIPLIED=new net.minecraft.client.renderer.RenderState.TransparencyState("wigo_translucent",()->{
        tint();RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(1,771,1,771);
    },()->{restoreTint();RenderSystem.disableBlend();RenderSystem.defaultBlendFunc();});
    private LegacyPanelType(){super("wigo_menu",()->{},()->{});}
    public static RenderType create(ResourceLocation id,boolean translucent) {
        return RenderTypeInvoker.wigoCreate("wigo_menu",DefaultVertexFormats.POSITION_TEX_COLOR,7,1536,
                RenderType.State.builder().setAlphaState(new net.minecraft.client.renderer.RenderState.AlphaState(1f/255f)).setShadeModelState(SMOOTH_SHADE)
                        .setTextureState(new net.minecraft.client.renderer.RenderState.TextureState(id,false,false))
                        .setTransparencyState(translucent?PREMULTIPLIED:OPAQUE).setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(translucent?COLOR_WRITE:COLOR_DEPTH_WRITE).setCullState(CULL).createCompositeState(false));
    }
}
