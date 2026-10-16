package dev.wigo.client;

import dev.wigo.client.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Click to place; Escape/F8 leave the original position untouched. */
public final class ObserverPositionScreen extends WigoScreen {
    public ObserverPositionScreen(WigoSettingsScreen previous){super(previous,"wigo.settings.observers.position");}
    @Override protected void init(){}
    @Override public void layoutWidgets(){}
    @Override public void renderBackground(com.mojang.blaze3d.vertex.PoseStack pose) {var g=GuiGraphics.wrap(pose);g.fill(0,0,width,height,0x90101010);}
    @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,int x,int y,float delta) {var g=GuiGraphics.wrap(pose);
        super.render(g.pose(),x,y,delta);
        g.drawCenteredString(font,new net.minecraft.network.chat.TranslatableComponent("wigo.settings.observers.place"),width/2,16,0xFFFFFFFF);
        g.drawCenteredString(font,new net.minecraft.network.chat.TranslatableComponent("wigo.settings.observers.cancel"),width/2,29,0xFFBBBBBB);
        ObserverBadge.paint(g,width,height,x,y,"0");
    }
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
        if(button!=0)return false;
        var layout=ObserverBadgeLayout.at(width,height,mouseX,mouseY,font.width("0"));
        Settings.observerX=(layout.eyeX()+ObserverBadgeLayout.EYE_WIDTH/2)/(double)width;
        Settings.observerY=(layout.y()+ObserverBadgeLayout.HEIGHT/2)/(double)height;
        Settings.save();onClose();return true;
    }
}
