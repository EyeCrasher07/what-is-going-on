package dev.wigo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class LegacySignModels {
    public static Object create(ResourceLocation texture) {
        String path=texture.getPath();String wood=path.substring("textures/entity/signs/".length(),path.length()-4);
        WoodType type=WoodType.values().filter(w->w.name().equals(wood)).findFirst().orElseThrow();
        return SignRenderer.createSignModel(Minecraft.getInstance().getEntityModels(),type);
    }
    public static void draw(GuiGraphics g,Object value,ResourceLocation texture,boolean standing) {
        var model=(SignRenderer.SignModel)value;model.stick.visible=standing;
        g.pose().pushPose();
        try {
            g.pose().translate(0,31,0);g.pose().scale(62.500004f,62.500004f,-62.500004f);
            model.root.render(g.pose(),g.bufferSource().getBuffer(RenderType.entityCutoutNoCull(texture)),15728880,OverlayTexture.NO_OVERLAY);
        } finally {g.pose().popPose();}
    }
}
