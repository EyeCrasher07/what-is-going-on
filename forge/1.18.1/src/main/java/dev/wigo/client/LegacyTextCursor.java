package dev.wigo.client;

import net.minecraft.client.gui.Font;
import dev.wigo.client.GuiGraphics;

/** Vanilla's pre-26.1 caret, reconstructed from its stable network description. */
public final class LegacyTextCursor {
    public static boolean isCursorVisible(long elapsed) {return elapsed/300%2==0;}
    public static void extractInsertCursor(GuiGraphics g,int x,int y,int color,int height) {g.fill(x,y-1,x+1,y+height,color);}
    public static void extractAppendCursor(GuiGraphics g,Font font,int x,int y,int color,boolean shadow) {g.drawString(font,"_",x,y,color,shadow);}
    private LegacyTextCursor() {}
}
