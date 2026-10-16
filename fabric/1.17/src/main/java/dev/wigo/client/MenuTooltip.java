package dev.wigo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Use the observer's native tooltip renderer and local item attributes. */
public final class MenuTooltip extends Screen {
    private static final MenuTooltip LOCAL = new MenuTooltip();
    private MenuTooltip() { super(new net.minecraft.network.chat.TextComponent("")); }
    public static void draw(GuiGraphics graphics, ItemStack stack, int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        LOCAL.init(mc, width, height);
        graphics.flush();
        LOCAL.renderTooltip(graphics.pose(), stack, x, y);
    }
}
