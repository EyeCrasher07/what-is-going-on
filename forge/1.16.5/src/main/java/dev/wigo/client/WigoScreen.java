package dev.wigo.client;

import dev.wigo.client.GuiGraphics;
import net.minecraft.client.gui.widget.button.Button;
import dev.wigo.client.LegacyTooltips;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;

/** Compact inventory-style windows; all WIGO screens retain the same opt-in sharing rule. */
public abstract class WigoScreen extends Screen {
    protected static final int TEXT=0xFF404040,SECONDARY=0xFF555555;
    protected final Screen previous;
    protected WigoScreen(Screen previous, String titleKey) {super(new net.minecraft.util.text.TranslationTextComponent(titleKey)); this.previous = previous;}
    public Screen previousScreen() {return previous instanceof WigoScreen settings ? settings.previousScreen() : previous;}
    protected int preferredWidth(){return 396;}
    protected int preferredHeight(){return 312;}
    protected int bodyWidth() {return Math.min(preferredWidth(), width - 24);}
    protected int bodyX() {return (width - bodyWidth()) / 2;}
    protected int bodyHeight(){return Math.min(preferredHeight(),height-16);}
    protected int bodyY(){return (height-bodyHeight())/2;}
    protected int bodyBottom(){return bodyY()+bodyHeight();}
    @Override public void render(com.mojang.blaze3d.matrix.MatrixStack pose,int x,int y,float delta) {var g=GuiGraphics.wrap(pose);
        renderBackground(g.pose());super.render(g.pose(),x,y,delta);for(var child:children())if(child instanceof LegacySelectionList<?> list)list.render(pose,x,y,delta);
    }
    protected <T extends LegacySelectionList<?>> T addMenuList(T list){return addWidget(list);}
    public abstract void layoutWidgets();
    protected void rebuildWidgets(){buttons.clear();children.clear();init();}
    protected void repositionElements() {layoutWidgets();}
    protected Button button(ITextComponent message, Runnable action, String help) {
        LegacyButtons.Builder builder = LegacyButtons.builder(message, b -> action.run()).bounds(0,0,100,20);
        if (help != null) builder.tooltip(LegacyTooltips.create(new net.minecraft.util.text.TranslationTextComponent(help)));
        return addButton(builder.build());
    }
    protected static ITextComponent toggle(String key, boolean value) {
        return new net.minecraft.util.text.TranslationTextComponent("wigo.settings.toggle", new net.minecraft.util.text.TranslationTextComponent(key), new net.minecraft.util.text.TranslationTextComponent(value ? "options.on" : "options.off"));
    }
    protected void heading(GuiGraphics g, String description) {
        g.drawString(font,title,(width-font.width(title))/2,bodyY()+12,TEXT,false);
        if (description != null) {
            var text=new net.minecraft.util.text.TranslationTextComponent(description);
            g.drawString(font,text,(width-font.width(text))/2,bodyY()+60,SECONDARY,false);
        }
    }
    protected void paragraph(GuiGraphics g, ITextComponent text, int y, int color) {
        y+=bodyY();
        for (var line : font.split(text, bodyWidth())) {
            g.drawString(font, line, bodyX(), y, color, false); y += 11;
        }
    }
    @Override public void renderBackground(com.mojang.blaze3d.matrix.MatrixStack pose) {var g=GuiGraphics.wrap(pose);
        if (minecraft.level == null) super.renderBackground(g.pose());
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
