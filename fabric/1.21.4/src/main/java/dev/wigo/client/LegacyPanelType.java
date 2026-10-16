package dev.wigo.client;

import com.mojang.blaze3d.vertex.*;
import dev.wigo.mixin.RenderTypeInvoker;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

/** Access vanilla's protected render states without changing their visibility. */
public final class LegacyPanelType extends RenderStateShard {
    private static float[] previousColor;
    private static void tint() {
        previousColor=com.mojang.blaze3d.systems.RenderSystem.getShaderColor().clone();
        float brightness=Settings.autoBrightness?1:Math.max(1,Settings.brightness/100f);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(brightness,brightness,brightness,1);
    }
    private static void restoreTint() {
        if(previousColor!=null)com.mojang.blaze3d.systems.RenderSystem.setShaderColor(previousColor[0],previousColor[1],previousColor[2],previousColor[3]);
        previousColor=null;
    }
    private static final TransparencyStateShard OPAQUE=new TransparencyStateShard("wigo_opaque",()->{
        tint();com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    },LegacyPanelType::restoreTint);
    private static final TransparencyStateShard PREMULTIPLIED=new TransparencyStateShard("wigo_chat",()->{
        tint();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    },()->{restoreTint();com.mojang.blaze3d.systems.RenderSystem.disableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();});
    private LegacyPanelType() {super("wigo_menu",()->{},()->{});}
    public static RenderType create(ResourceLocation id,boolean chat) {
        return RenderTypeInvoker.wigoCreate("wigo_menu",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,1536,
                RenderType.CompositeState.builder().setShaderState(POSITION_TEXTURE_COLOR_SHADER)
                        .setTextureState(new TextureStateShard(id,TriState.FALSE,false))
                        .setTransparencyState(chat?PREMULTIPLIED:OPAQUE).setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(chat?COLOR_WRITE:COLOR_DEPTH_WRITE).setCullState(CULL).createCompositeState(false));
    }
}
