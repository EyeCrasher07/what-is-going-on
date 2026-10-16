package dev.wigo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/** Snapshot menus resolve tooltip artwork and item details on the observer's client. */
public final class MenuTooltip {
    public static void draw(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int width, int height) {
        try (GuiViewport viewport = GuiViewport.open(width, height)) {
            graphics.setTooltipForNextFrame(Minecraft.getInstance().font, stack, x, y);
            graphics.extractDeferredElements(x, y, 0);
        }
    }
}
