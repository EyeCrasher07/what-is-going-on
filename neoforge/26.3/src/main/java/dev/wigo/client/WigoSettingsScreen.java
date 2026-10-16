package dev.wigo.client;

import dev.wigo.config.SharingRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.IntConsumer;

public final class WigoSettingsScreen extends WigoScreen {
    private static final int ROW_STEP=21;
    public enum Tab { MINE, OTHERS }
    private Tab tab;
    private Button mine, others, done, reset;
    private int controlScroll,visibleRows,firstRowY;
    private boolean draggingScroll;
    private double scrollGrab;
    private int policyMaximum;
    private boolean policyPersonal,policyCounter,policySmooth;
    private final List<AbstractWidget> controls = new ArrayList<>();
    public WigoSettingsScreen(Screen previous) {this(previous, Tab.MINE);}
    public WigoSettingsScreen(Screen previous, Tab tab) {super(previous,"wigo.settings.title"); this.tab = tab;}
    public Tab tab() {return tab;}
    public void selectTab(Tab value) {tab = value; controlScroll=0; rebuildWidgets();}
    public void resetSettings() {
        if(tab==Tab.MINE){Settings.resetMyMenus();WigoClient.observerCount=0;}
        else {Settings.resetOtherMenus();PanelRenderer.close();}
        rebuildWidgets();
    }
    @Override protected void init() {
        controls.clear();
        draggingScroll=false;
        policyMaximum=WigoClient.maxSharingDistance();policyPersonal=WigoClient.personalDistance();
        policyCounter=WigoClient.allowsObserverCounter();policySmooth=WigoClient.allowsSmoothMode();
        mine = button(Component.translatable("wigo.settings.tab.mine"), () -> selectTab(Tab.MINE), "wigo.settings.tab.mine.description");
        others = button(Component.translatable("wigo.settings.tab.others"), () -> selectTab(Tab.OTHERS), "wigo.settings.tab.others.description");
        mine.active = tab != Tab.MINE; others.active = tab != Tab.OTHERS;
        if (tab == Tab.MINE) {
            controls.add(button(toggle("wigo.settings.share",Settings.share), () -> {Settings.share = !Settings.share; Settings.save(); rebuildWidgets();}, "wigo.settings.share.description"));
            var menus=addRenderableWidget(new WigoIconButton(Component.empty(),b->minecraft.gui.setScreen(new MenuSharingScreen(this)),null,true));
            menus.setTooltip(Tooltip.create(Component.translatable("wigo.settings.menus.description")));controls.add(menus);
            controls.add(button(Component.translatable("wigo.settings.audience", Component.translatable(modeKey(Settings.rules().mode()))),
                    () -> minecraft.gui.setScreen(new PlayerSharingScreen(this)), "wigo.settings.audience.description"));
            controls.add(button(Component.translatable("wigo.settings.side",Component.translatable(sideKey(Settings.rules().side()))), () -> {
                var choices = SharingRules.Side.values(); Settings.side(choices[(Settings.rules().side().ordinal()+1)%choices.length]); rebuildWidgets();
            }, "wigo.settings.side.description"));
            var counter=button(toggle("wigo.settings.observers",Settings.showObservers&&policyCounter),()->{
                Settings.showObservers=!Settings.showObservers;WigoClient.observerCount=0;Settings.save();rebuildWidgets();
            },policyCounter?"wigo.settings.observers.description":"wigo.settings.observers.locked");
            counter.active=policyCounter;controls.add(counter);
            var position=addRenderableWidget(new WigoIconButton(Component.empty(),b->minecraft.gui.setScreen(new ObserverPositionScreen(this)),null,true));
            position.active=policyCounter;
            position.setTooltip(Tooltip.create(Component.translatable(policyCounter?"wigo.settings.observers.position":"wigo.settings.observers.locked")));controls.add(position);
            controls.add(addRenderableWidget(new DistanceSlider()));
            controls.add(addRenderableWidget(WigoIconButton.cursorButton(Component.translatable("wigo.settings.cursor",
                    Component.translatable(Settings.cursorStyle.label())),b->minecraft.gui.setScreen(new CursorSelectionScreen(this)),Settings.cursorStyle)));
        } else {
            controls.add(button(toggle("wigo.settings.receive",Settings.receive), () -> {
                Settings.receive = !Settings.receive; Settings.save();
                if (!Settings.receive) {WigoClient.menus.clear(); PanelRenderer.close();} rebuildWidgets();
            }, "wigo.settings.receive.description"));
            var viewing=button(Component.translatable("wigo.settings.viewing",Component.translatable(Settings.smoothMenus&&policySmooth
                    ?(WigoClient.connected&&!WigoClient.supportsSmoothMenus()?"wigo.settings.viewing.unavailable":"wigo.settings.viewing.smooth")
                    :"wigo.settings.viewing.economical")), () -> {
                Settings.smoothMenus=!Settings.smoothMenus;Settings.save();rebuildWidgets();
            },policySmooth?"wigo.settings.viewing.description":"wigo.settings.viewing.locked");
            viewing.active=policySmooth;controls.add(viewing);
            controls.add(button(toggle("wigo.settings.unsupported",Settings.showUnsupportedMenus), () -> {
                Settings.showUnsupportedMenus = !Settings.showUnsupportedMenus; Settings.save(); rebuildWidgets();
            }, "wigo.settings.unsupported.description"));
            slider("transparency",Settings.transparency,0,100,v -> Settings.transparency = v);
            controls.add(button(toggle("wigo.settings.autoBrightness",Settings.autoBrightness),()->{
                Settings.autoBrightness=!Settings.autoBrightness;Settings.save();rebuildWidgets();
            },"wigo.settings.autoBrightness.description"));
            slider("brightness",Settings.brightness,0,200,v -> Settings.brightness = v);
            slider("size",Settings.size,50,200,v -> Settings.size = v);
            slider("panelOffset",Settings.panelOffset,-200,200,v -> Settings.panelOffset = v);
            slider("panelPitch",Settings.panelPitch,-60,60,v -> Settings.panelPitch = v);
            controls.add(button(Component.translatable("wigo.settings.placement.reset"),()->{
                Settings.panelOffset=0;Settings.panelPitch=0;Settings.save();rebuildWidgets();
            },"wigo.settings.placement.reset.description"));
            slider("curvature",Settings.reverseCurvature?-Settings.curvature:Settings.curvature,-100,100,v -> {
                Settings.curvature=Math.abs(v);Settings.reverseCurvature=v<0;
            });
            controls.add(button(toggle("wigo.settings.mirror",Settings.realisticMirroring), () -> {
                Settings.realisticMirroring = !Settings.realisticMirroring; Settings.save(); rebuildWidgets();
            }, "wigo.settings.mirror.description"));
        }
        done = button(Component.translatable("gui.done"), this::onClose, null);
        reset=addRenderableWidget(WigoIconButton.resetButton(b->minecraft.gui.setScreen(new WigoResetScreen(this,tab))));
        reset.setTooltip(Tooltip.create(Component.translatable("wigo.settings.reset.description")));
        layoutWidgets();
    }
    private void slider(String key,int current,int min,int max,IntConsumer change) {
        var slider = new AppearanceSlider(key,current,min,max,change);
        controls.add(addRenderableWidget(slider));
    }
    @Override public void layoutWidgets() {
        int x = bodyX(), w = bodyWidth(), top=bodyY();
        mine.setRectangle((w-4)/2,20,x,top+30); others.setRectangle((w-4)/2,20,x+(w+4)/2,top+30);
        if(tab == Tab.MINE) {
            firstRowY=top+(bodyHeight()<300?60:80);
            visibleRows=Math.max(1,Math.min(6,(bodyBottom()-32-firstRowY+ROW_STEP-20)/ROW_STEP));
            int offset=rowOffset();
            for(int i=0;i<controls.size();i++) {
                int row=i<2?0:i<4?i-1:i<6?3:i-2;
                int y=firstRowY+(row-offset)*ROW_STEP;
                controls.get(i).setRectangle(i==0||i==4?w-28:i==1||i==5?24:w,20,i==1||i==5?x+w-24:x,y);
                controls.get(i).visible=row>=offset&&row<offset+visibleRows;
            }
        } else {
            firstRowY=top+(bodyHeight()>=300?80:60);
            int bottom=bodyBottom()-32;
            visibleRows=Math.max(1,Math.min(controls.size(),(bottom-firstRowY+ROW_STEP-20)/ROW_STEP));
            int offset=rowOffset();
            for(int i=0;i<controls.size();i++) {
                var widget=controls.get(i);widget.setRectangle(w,20,x,firstRowY+(i-offset)*ROW_STEP);
                widget.visible=i>=offset&&i<offset+visibleRows;
            }
        }
        done.setRectangle(w-28,20,x,bodyBottom()-26);
        reset.setRectangle(24,20,x+w-24,bodyBottom()-26);
    }
    private int rowCount(){return tab==Tab.MINE?6:controls.size();}
    private int rowOffset(){return Math.clamp(controlScroll,0,Math.max(0,rowCount()-visibleRows));}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(visibleRows<rowCount()&&x>=bodyX()&&x<=bodyX()+bodyWidth()+10
                &&y>=firstRowY&&y<bodyBottom()-32&&vertical!=0) {
            scrollTo(rowOffset()+(vertical<0?1:-1));
            return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    private int scrollTrack(){return visibleRows*ROW_STEP-(ROW_STEP-20);}
    private int scrollThumb(){return Math.max(12,scrollTrack()*visibleRows/rowCount());}
    private int scrollTop(){return firstRowY+(scrollTrack()-scrollThumb())*rowOffset()/(rowCount()-visibleRows);}
    private void scrollTo(int row){
        controlScroll=Math.clamp(row,0,Math.max(0,rowCount()-visibleRows));layoutWidgets();
        if(getFocused() instanceof AbstractWidget widget&&!widget.visible)setFocused(null);
    }
    private void dragScroll(double y){
        int travel=scrollTrack()-scrollThumb();
        if(travel>0)scrollTo((int)Math.round((y-firstRowY-scrollGrab)*(rowCount()-visibleRows)/travel));
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean doubled){
        double x=event.x(),y=event.y();int edge=bodyX()+bodyWidth()+4;
        if(event.buttonInfo().button()==1&&visibleRows<rowCount()&&x>=edge-2&&x<edge+5&&y>=firstRowY&&y<firstRowY+scrollTrack()){
            int top=scrollTop();scrollGrab=y>=top&&y<top+scrollThumb()?y-top:scrollThumb()/2.0;
            draggingScroll=true;setFocused(null);dragScroll(y);return true;
        }
        return super.mouseClicked(event,doubled);
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,double dx,double dy){
        if(draggingScroll){dragScroll(event.y());return true;}return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event){
        if(draggingScroll&&event.buttonInfo().button()==1){draggingScroll=false;return true;}return super.mouseReleased(event);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta) {
        super.extractRenderState(g,x,y,delta);
        heading(g,bodyHeight()<300?null:tab == Tab.MINE ? "wigo.settings.mine.subtitle" : "wigo.settings.others.subtitle");
        if(visibleRows<rowCount()) {
            int x1=bodyX()+bodyWidth()+4,top=scrollTop();
            g.fill(x1,firstRowY,x1+3,firstRowY+scrollTrack(),0xFF343D49);g.fill(x1,top,x1+3,top+scrollThumb(),0xFF98A5B6);
        }

    }
    public static String modeKey(SharingRules.Mode mode) {return "wigo.settings.mode."+mode.name().toLowerCase(Locale.ROOT);}
    @Override public void tick(){
        if(policyCounter!=WigoClient.allowsObserverCounter()||policySmooth!=WigoClient.allowsSmoothMode()
                ||tab==Tab.MINE&&(policyMaximum!=WigoClient.maxSharingDistance()||policyPersonal!=WigoClient.personalDistance()))rebuildWidgets();
    }
    private static final class DistanceSlider extends AbstractSliderButton {
        private final int maximum;
        DistanceSlider(){
            super(0,0,100,20,Component.empty(),WigoClient.maxSharingDistance()==1?0:(WigoClient.sharingDistance()-1)/(double)(WigoClient.maxSharingDistance()-1));
            maximum=WigoClient.maxSharingDistance();active=WigoClient.personalDistance()&&maximum>1;
            setTooltip(Tooltip.create(Component.translatable(WigoClient.personalDistance()?"wigo.settings.distance.description":"wigo.settings.distance.locked",maximum)));
            updateMessage();
        }
        @Override protected void updateMessage(){setMessage(Component.translatable("wigo.settings.distance",WigoClient.sharingDistance()));}
        @Override protected void applyValue(){Settings.sharingDistance=1+(int)Math.round(value*(maximum-1));updateMessage();}
        @Override public void onRelease(net.minecraft.client.input.MouseButtonEvent event){super.onRelease(event);Settings.save();}
        @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){boolean used=super.keyPressed(event);if(used)Settings.save();return used;}
    }
    public static String sideKey(SharingRules.Side side) {return "wigo.settings.side."+side.name().toLowerCase(Locale.ROOT);}
    private static final class AppearanceSlider extends AbstractSliderButton {
        private final String key; private final int min,max; private final IntConsumer change;
        AppearanceSlider(String key,int current,int min,int max,IntConsumer change) {
            super(0,0,100,20,Component.empty(),(current-min)/(double)(max-min));
            this.key=key;this.min=min;this.max=max;this.change=change;
            active=!key.equals("brightness")||!Settings.autoBrightness;
            setTooltip(Tooltip.create(Component.translatable("wigo.settings."+key+".description")));updateMessage();
        }
        private int amount() {return min+(int)Math.round(value*(max-min));}
        @Override protected void updateMessage() {
            if(key.equals("brightness")&&Settings.autoBrightness)setMessage(Component.translatable("wigo.settings.brightness.automatic"));
            else if(key.equals("panelOffset"))setMessage(Component.translatable("wigo.settings.panelOffset.value",String.format(Locale.ROOT,"%+.2f",amount()/100.0)));
            else if(key.equals("panelPitch"))setMessage(Component.translatable("wigo.settings.panelPitch.value",(amount()>0?"+":"")+amount()));
            else if(key.equals("curvature"))setMessage(Component.translatable("wigo.settings.percent",Component.translatable("wigo.settings.curvature"),(amount()>0?"+":"")+amount()));
            else setMessage(Component.translatable("wigo.settings.percent",Component.translatable("wigo.settings."+key),amount()));
        }
        @Override protected void applyValue() {if(active)change.accept(amount());}
        @Override public void onRelease(net.minecraft.client.input.MouseButtonEvent event) {super.onRelease(event);Settings.save();}
        @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
            if(!active)return false;
            boolean result=super.keyPressed(event);if(result)Settings.save();return result;
        }
    }
    @Override public void onClose() {Settings.save();super.onClose();}
}
