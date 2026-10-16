package dev.wigo.client;

import dev.wigo.client.GuiGraphics;
import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.list.AbstractOptionList;
import java.util.function.Consumer;

/** Older list entries receive their bounds during render, rather than acting as widgets. */
public abstract class LegacyListEntry<E extends LegacyListEntry<E>> extends AbstractOptionList.Entry<E> {
    private int x,y,width;
    public void setX(int value) {x=value;}
    public void setY(int value) {y=value;}
    public void setWidth(int value) {width=value;}
    public int getContentX() {return x;}
    public int getContentY() {return y;}
    public int getContentRight() {return x+width;}
    public int getContentWidth() {return width;}
    public abstract void visitWidgets(Consumer<Widget> visitor);
    public abstract void renderContent(GuiGraphics g,int mouseX,int mouseY,boolean hovered,float delta);
    @Override public final void render(com.mojang.blaze3d.matrix.MatrixStack pose,int index,int y,int x,int width,int height,int mouseX,int mouseY,boolean hovered,float delta) {var g=GuiGraphics.wrap(pose);
        setX(x);setY(y);setWidth(width);renderContent(g,mouseX,mouseY,hovered,delta);
    }
    public static void resize(AbstractList<?> list,int width,int height,int x,int y) {list.updateSize(width,y+height,y,y+height);list.setLeftPos(x);layout(list);}
    public static void layout(AbstractList<?> list) {
        for(int i=0;i<list.children().size();i++) if(list.children().get(i) instanceof LegacyListEntry<?> row) {
            row.setX(list.getRowLeft());row.setY(((dev.wigo.mixin.SelectionListAccessor)list).wigoRowTop(i));row.setWidth(list.getRowWidth());
        }
    }
}
