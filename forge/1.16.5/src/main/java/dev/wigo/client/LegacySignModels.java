package dev.wigo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.SignTileEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.block.WoodType;

public final class LegacySignModels {
    public static Object create(ResourceLocation texture) {
        return new SignTileEntityRenderer.SignModel();
    }
    public static void draw(GuiGraphics g,Object value,ResourceLocation texture,boolean standing) {
        var model=(SignTileEntityRenderer.SignModel)value;model.stick.visible=standing;
        g.pose().pushPose();
        try {
            g.pose().translate(0,31,0);g.pose().scale(62.500004f,62.500004f,-62.500004f);
            model.renderToBuffer(g.pose(),g.bufferSource().getBuffer(RenderType.entityCutoutNoCull(texture)),15728880,OverlayTexture.NO_OVERLAY,1,1,1,1);
        } finally {g.pose().popPose();}
    }
}
