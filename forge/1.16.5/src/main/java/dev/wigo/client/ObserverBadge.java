package dev.wigo.client;

import net.minecraft.client.Minecraft;
import dev.wigo.client.GuiGraphics;
import net.minecraft.client.gui.screen.Screen;

/** Owner-only overlay, appended after the shared GUI description has finished. */
public final class ObserverBadge {
    private static final String[] EYE={"    #####    ","  #########  "," ##.......## ","##...###...##","#....###....#","##...###...##"," ##.......## ","  #########  ","    #####    "};
    private ObserverBadge(){}
    public static void draw(GuiGraphics g,Screen screen) {
        if(!WigoClient.allowsObserverCounter()||!Settings.showObservers||!Settings.sharing()||!WigoClient.connected||screen instanceof ObserverPositionScreen||GuiCapture.active())return;
        String kind=GuiCapture.classify(screen),menu=MenuIds.of(screen,kind);
        if(kind.equals("none")||!Settings.menuEnabled(menu)||!WigoClient.serverAllows(menu))return;
        paint(g,screen.width,screen.height,Settings.observerX*screen.width,Settings.observerY*screen.height,
                WigoClient.supportsObserverCounts()?Integer.toString(WigoClient.observerCount):"—");
    }
    public static ObserverBadgeLayout paint(GuiGraphics g,int width,int height,double x,double y,String number) {
        var font=Minecraft.getInstance().font;
        var layout=ObserverBadgeLayout.at(width,height,x,y,font.width(number));
        g.fill(layout.left()-2,layout.y()-2,layout.right()+2,layout.y()+ObserverBadgeLayout.HEIGHT+2,0xA0101010);
        for(int row=0;row<EYE.length;row++)for(int col=0;col<EYE[row].length();col++) {
            char pixel=EYE[row].charAt(col);
            if(pixel!=' ')g.fill(layout.eyeX()+col,layout.y()+row,layout.eyeX()+col+1,layout.y()+row+1,pixel=='#'?0xFFF0F0F0:0xFF404040);
        }
        g.drawString(font,number,layout.numberX(),layout.y(),0xFFFFFFFF,true);
        return layout;
    }
}
