package dev.wigo.client;

import dev.wigo.client.GuiGraphics;

/** Fit transient descriptions without changing the persistent menu's canvas. */
public final class LegacyTooltipLayout {
    public static void draw(GuiGraphics graphics,int width,int height,int desiredX,int desiredY,int originX,int originY,Runnable draw) {
        var canvas=GuiCapture.tooltipLocal(graphics);
        if(canvas==null||width<=0||height<=0) {draw.run();return;}
        float border=12,scale=Math.min(1,Math.min((canvas.width()-2f)/(width+2*border),(canvas.height()-2f)/(height+2*border)));
        float minX=canvas.left()+1+border*scale,minY=canvas.top()+1+border*scale;
        float left=Math.max(minX,Math.min(desiredX,canvas.right()-1-(width+border)*scale));
        float top=Math.max(minY,Math.min(desiredY,canvas.bottom()-1-(height+border)*scale));
        graphics.pose().pushPose();graphics.pose().translate(left-originX*scale,top-originY*scale,0);graphics.pose().scale(scale,scale,1);
        try(var overlay=GuiCapture.overlay()) {draw.run();}
        finally {graphics.pose().popPose();}
    }
    private LegacyTooltipLayout() {}
}
