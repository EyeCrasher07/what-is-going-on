package dev.wigo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;

/** The view angle belongs to the widget, not to the real player's camera or body. */
public final class SettingsPreviewWidget extends AbstractWidget {
    private float yaw=145, elevation=12, zoom=1;
    private boolean rotating=true, moved;
    private long lastFrame;
    private final PanelLighting lighting=new PanelLighting();

    public SettingsPreviewWidget() {super(0,0,160,180,Component.translatable("wigo.preview.title"));}

    @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float delta) {
        // Sharing F8 never captures a recursively nested 3D preview.
        if(GuiCapture.active())return;
        var mc=Minecraft.getInstance();
        int x=getX(),y=getY(),w=getWidth(),h=getHeight();
        long now=System.nanoTime();
        if(rotating&&lastFrame!=0)yaw=(yaw+(float)Math.min((now-lastFrame)/1_000_000_000.0,0.1)*18)%360;
        lastFrame=now;
        var help=Component.translatable(rotating?"wigo.preview.rotate":"wigo.preview.paused");
        var lines=mc.font.split(help,w-12);
        int footer=lines.size()*10+6;
        for(int i=0;i<lines.size();i++)g.drawString(mc.font,lines.get(i),x+(w-mc.font.width(lines.get(i)))/2,y+h-footer+i*10,0xFFE0E0E0,true);
        if(mc.player==null) {
            return;
        }
        var player=SettingsPreviewRenderer.player();if(player==null)return;
        int top=y+2,bottom=y+h-footer-5;
        // Fixed camera scale makes size/height changes visible rather than fitting them away.
        float scale=zoom*Math.min((w-8)/3.6f,Math.max(1,bottom-top)/3.6f);
        int light=mc.level==null?15:PanelRenderer.ambientLight(mc.level,mc.player.blockPosition());
        int brightness=lighting.update(Settings.autoBrightness,Settings.brightness,light,now);
        var state=new SettingsPreviewState(player,yaw,elevation,Settings.transparency,brightness,Settings.size,
                Settings.curvature,Settings.reverseCurvature,Settings.realisticMirroring,Settings.panelOffset,
                Settings.panelPitch,Settings.cursorStyle,x+3,top,x+w-3,bottom,scale,
                new ScreenRectangle(x+3,top,w-6,Math.max(1,bottom-top)));
        SettingsPreviewRenderer.render(g,state);
    }

    @Override public void onClick(double mouseX,double mouseY) {moved=false;}
    @Override protected void onDrag(double mouseX,double mouseY,double dx,double dy) {
        if(dx==0&&dy==0)return;
        moved=true;rotating=false;yaw=(yaw+(float)dx*1.5f)%360;elevation=dev.wigo.protocol.LegacyClamp.clamp(elevation+(float)dy,-45,45);
    }
    @Override public void onRelease(double mouseX,double mouseY) {if(!moved)rotating=!rotating;lastFrame=0;}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(!isMouseOver(x,y)||vertical==0)return false;
        zoom=dev.wigo.protocol.LegacyClamp.clamp(zoom*(float)Math.pow(1.12,vertical),.35f,1.8f);return true;
    }
    @Override public boolean keyPressed(int key,int scan,int mods) {
        if(key==257||key==32||key==335){rotating=!rotating;return true;}
        if(key==263||key==262){rotating=false;yaw+=key==263?-10:10;return true;}
        if(key==265||key==264){rotating=false;elevation=Math.max(-45,Math.min(45,elevation+(key==265?-5:5)));return true;}
        return super.keyPressed(key,scan,mods);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {defaultButtonNarrationText(output);}
}
