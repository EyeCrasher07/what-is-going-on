package dev.wigo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL11;

/** Item depth belongs to that model, not to subsequently drawn count text. */
public final class LegacyReplayLayers {
    public static void advance(GuiGraphics graphics) {
        graphics.flush();
        if(GuiScratch.target()==null)return;
        boolean writes=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        RenderSystem.depthMask(true);GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);RenderSystem.depthMask(writes);
    }
}
