package dev.wigo.client;

import net.minecraft.world.item.ItemStack;

public final class MenuTooltip {
    public static void draw(GuiGraphics graphics, ItemStack stack, int x, int y, int width, int height) {
        LegacyTooltip.draw(graphics, stack, x, y, width, height);
    }
}
