package dev.wigo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

/** Flatten completed native models before the next semantic GUI layer. */
public final class LegacyReplayLayers {
    public static void advance(GuiGraphics graphics) {
        graphics.flush();
        if(GuiScratch.target()!=null)RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(GuiScratch.target().getDepthTexture(),1);
    }
}
