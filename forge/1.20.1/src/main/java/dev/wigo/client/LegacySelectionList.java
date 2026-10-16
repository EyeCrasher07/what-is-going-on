package dev.wigo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

/** Bounds and visibility for lists that predate AbstractWidget inheritance. */
public abstract class LegacySelectionList<E extends ContainerObjectSelectionList.Entry<E>> extends ContainerObjectSelectionList<E> {
    public boolean visible=true;
    protected LegacySelectionList(Minecraft mc,int width,int height,int y,int rowHeight) {
        super(mc,width,y+height,y,y+height,rowHeight);
        setRenderBackground(false);setRenderTopAndBottom(false);
    }
    public int getWidth(){return width;}
    public int getHeight(){return y1-y0;}
    public int getX(){return x0;}
    public int getY(){return y0;}
    public int getRight(){return x1;}
    public int getBottom(){return y1;}
    @Override public void render(GuiGraphics g,int x,int y,float delta) {
        if(!visible)return;
        g.enableScissor(getX(),getY(),getRight(),getBottom());
        try {super.render(g,x,y,delta);}finally {g.disableScissor();}
    }
    @Override public boolean isMouseOver(double x,double y){return visible&&super.isMouseOver(x,y);}
    @Override public boolean mouseClicked(double x,double y,int button){return visible&&super.mouseClicked(x,y,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return visible&&super.mouseReleased(x,y,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return visible&&super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseScrolled(double x,double y,double vertical){return visible&&super.mouseScrolled(x,y,vertical);}
}
