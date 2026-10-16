package dev.wigo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.item.ItemStack;

/** Build snapshot-container tooltips from the observer's own items, font and artwork. */
public final class LegacyTooltip extends Screen {
    private static final LegacyTooltip LOCAL = new LegacyTooltip();
    private LegacyTooltip() { super(new StringTextComponent("")); }
    public static void draw(GuiGraphics graphics, ItemStack stack, int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        LOCAL.minecraft = mc; LOCAL.font = mc.font;
        LOCAL.width = width; LOCAL.height = height;
        graphics.flush();
        LOCAL.renderTooltip(graphics.pose(), stack, x, y);
    }
}
