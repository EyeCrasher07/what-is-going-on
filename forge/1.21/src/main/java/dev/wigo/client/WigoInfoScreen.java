package dev.wigo.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.net.URI;
import java.util.*;

public final class WigoInfoScreen extends WigoScreen {
    public static final List<URI> LINKS=List.of(
        URI.create("https://guns.lol/eyecrasher"),
        URI.create("https://github.com/EyeCrasher07/what-is-going-on"),
        URI.create("https://github.com/EyeCrasher07/what-is-going-on/issues"),
        URI.create("https://modrinth.com/project/what-is-going-on"),
        URI.create("https://www.curseforge.com/minecraft/mc-mods/what-is-going-on-wigo"));
    private final List<Button> links=new ArrayList<>();
    private Button done;
    private int scroll;
    public WigoInfoScreen(WigoScreen previous){super(previous,"wigo.info.title");}
    @Override protected int preferredWidth(){return 296;}
    @Override protected int preferredHeight(){return 252;}
    @Override protected void init(){
        links.clear();
        String[] labels={"Contact","Source code","Issues","Modrinth","CurseForge"};
        for(int i=0;i<labels.length;i++) {
            int index=i;String icon=switch(i){case 0->"face";case 1->"github";case 2->"issues";case 3->"modrinth";case 4->"curseforge";default->null;};
            links.add(addRenderableWidget(new WigoIconButton(Component.literal(labels[i]),b->WigoLinks.open(this,LINKS.get(index)),
                    icon==null?null:ResourceLocation.fromNamespaceAndPath("wigo","textures/gui/"+icon+".png"),false)));
        }
        done=button(Component.translatable("gui.done"),this::onClose,null);layoutWidgets();
    }
    private int contentTop(){return bodyY()+32;}
    private int contentBottom(){return bodyBottom()-38;}
    private int maxScroll(){return Math.max(0,154-(contentBottom()-contentTop()));}
    @Override public void layoutWidgets(){
        scroll=Math.clamp(scroll,0,maxScroll());int x=bodyX(),y=contentTop()-scroll,w=bodyWidth();
        for(int i=0;i<3;i++)links.get(i).setRectangle(w,20,x,y+i*24);
        links.get(3).setRectangle((w-6)/2,20,x,y+72);
        links.get(4).setRectangle((w-6)/2,20,x+(w+6)/2,y+72);
        for(var button:links)button.visible=button.getY()>=contentTop()&&button.getBottom()<=contentBottom();
        done.setRectangle(w,20,x,bodyBottom()-28);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(x>=bodyX()&&x<=bodyX()+bodyWidth()+8&&y>=contentTop()&&y<contentBottom()&&vertical!=0&&maxScroll()>0){
            scroll=Math.clamp(scroll+(vertical<0?12:-12),0,maxScroll());layoutWidgets();
            if(getFocused() instanceof net.minecraft.client.gui.components.AbstractWidget widget&&!widget.visible)setFocused(null);return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public void render(GuiGraphics g,int x,int y,float delta){
        super.render(g,x,y,delta);heading(g,null);
        int at=contentTop()+106-scroll;
        g.enableScissor(bodyX(),contentTop(),bodyX()+bodyWidth(),contentBottom());
        line(g,Component.translatable(WigoClient.connected?"wigo.settings.server.connected":"wigo.settings.server.missing"),at);
        line(g,Component.translatable("wigo.settings.received",GuiCapture.active()?"—":WigoClient.receivedBytes/1024),at+12);
        line(g,Component.translatable("wigo.info.version",BuildInfo.version()),at+24);
        var update=UpdateChecker.result();
        int statusColor=switch(update.state()) {case CURRENT->0xFF008800;case AVAILABLE->0xFF9B7400;default->TEXT;};
        line(g,Component.translatable("wigo.info.update."+update.state().name().toLowerCase(Locale.ROOT),update.version()),at+36,statusColor);
        g.disableScissor();
        if(maxScroll()>0){
            int track=contentBottom()-contentTop(),thumb=Math.max(12,track*track/154);
            int top=contentTop()+(track-thumb)*scroll/maxScroll();
            g.fill(bodyX()+bodyWidth()+3,contentTop(),bodyX()+bodyWidth()+6,contentBottom(),0xFF777777);
            g.fill(bodyX()+bodyWidth()+3,top,bodyX()+bodyWidth()+6,top+thumb,0xFFEEEEEE);
        }
    }
    private void line(GuiGraphics g,Component text,int y){line(g,text,y,TEXT);}
    private void line(GuiGraphics g,Component text,int y,int color){if(y>=contentTop()&&y+9<=contentBottom())g.drawString(font,text,bodyX(),y,color,false);}
}
