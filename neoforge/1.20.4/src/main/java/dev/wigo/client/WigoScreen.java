package dev.wigo.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact inventory-style windows; all WIGO screens retain the same opt-in sharing rule. */
public abstract class WigoScreen extends Screen {
    protected static final int TEXT=0xFF404040,SECONDARY=0xFF555555;
    protected final Screen previous;
    protected WigoScreen(Screen previous, String titleKey) {super(Component.translatable(titleKey)); this.previous = previous;}
    public Screen previousScreen() {return previous instanceof WigoScreen settings ? settings.previousScreen() : previous;}
    protected int preferredWidth(){return 400;}
    protected int preferredHeight(){return 312;}
    protected int bodyWidth() {return Math.min(preferredWidth(), width - 24);}
    protected int bodyX() {return (width - bodyWidth()) / 2;}
    protected int bodyHeight(){return Math.min(preferredHeight(),height-16);}
    protected int bodyY(){return (height-bodyHeight())/2;}
    protected int bodyBottom(){return bodyY()+bodyHeight();}
    @Override public void render(GuiGraphics g,int x,int y,float delta) {
        renderBackground(g,x,y,delta);super.render(g,x,y,delta);
    }
    public abstract void layoutWidgets();
    @Override protected void repositionElements() {layoutWidgets();}
    protected Button button(Component message, Runnable action, String help) {
        Button.Builder builder = Button.builder(message, b -> action.run()).bounds(0,0,100,20);
        if (help != null) builder.tooltip(Tooltip.create(Component.translatable(help)));
        return addRenderableWidget(builder.build());
    }
    protected static Component toggle(String key, boolean value) {
        return Component.translatable("wigo.settings.toggle", Component.translatable(key), Component.translatable(value ? "options.on" : "options.off"));
    }
    protected void heading(GuiGraphics g, String description) {
        g.drawString(font,title,(width-font.width(title))/2,bodyY()+12,TEXT,false);
        if (description != null) {
            var text=Component.translatable(description);
            g.drawString(font,text,(width-font.width(text))/2,bodyY()+60,SECONDARY,false);
        }
    }
    protected void paragraph(GuiGraphics g, Component text, int y, int color) {
        y+=bodyY();
        for (var line : font.split(text, bodyWidth())) {
            g.drawString(font, line, bodyX(), y, color, false); y += 11;
        }
    }
    @Override public void renderBackground(GuiGraphics g, int x, int y, float delta) {
        if (minecraft.level == null) super.renderBackground(g,x,y,delta);
        else g.fill(0,0,width,height,0x90101010);
        int left=bodyX()-10,right=bodyX()+bodyWidth()+10,top=bodyY(),bottom=bodyBottom();
        // The vanilla inventory bevel, with a plain interior and no item slots.
        g.fill(left+1,top,right-1,bottom,0xFF000000);
        g.fill(left,top+1,right,bottom-1,0xFF000000);
        g.fill(left+2,top+2,right-2,bottom-2,0xFFC6C6C6);
        g.fill(left+2,top+2,right-2,top+4,0xFFFFFFFF);
        g.fill(left+2,top+2,left+4,bottom-2,0xFFFFFFFF);
        g.fill(left+2,bottom-4,right-2,bottom-2,0xFF555555);
        g.fill(right-4,top+2,right-2,bottom-2,0xFF555555);
    }
    @Override public void onClose() {minecraft.setScreen(previous);}
    @Override public boolean isPauseScreen() {return false;}
}
