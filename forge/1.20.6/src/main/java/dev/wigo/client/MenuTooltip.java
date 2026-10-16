package dev.wigo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** Use the observer's native tooltip renderer and local item attributes. */
public final class MenuTooltip {
    public static void draw(GuiGraphics graphics, ItemStack stack, int x, int y, int width, int height) {
        try (GuiViewport viewport = GuiViewport.open(width, height)) {
            graphics.renderTooltip(Minecraft.getInstance().font, stack, x, y);
        }
    }
}
