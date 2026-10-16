package dev.wigo.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Reset one settings tab only after an explicit confirmation. */
public final class WigoResetScreen extends WigoScreen {
    private final WigoSettingsScreen settings;
    private final WigoSettingsScreen.Tab tab;
    private Button yes,no;
    public WigoResetScreen(WigoSettingsScreen settings,WigoSettingsScreen.Tab tab){
        super(settings,"wigo.settings.reset.title");this.settings=settings;this.tab=tab;
    }
    @Override protected int preferredHeight(){return 170;}
    @Override protected void init(){
        yes=button(Component.translatable("gui.yes"),()->{
            settings.resetSettings();
            minecraft.setScreen(settings);
        },null);
        no=button(Component.translatable("gui.no"),this::onClose,null);
        layoutWidgets();
    }
    @Override public void layoutWidgets(){
        int x=bodyX(),w=bodyWidth(),y=bodyBottom()-30;
        yes.setRectangle((w-6)/2,20,x,y);no.setRectangle((w-6)/2,20,x+(w+6)/2,y);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        super.extractRenderState(g,x,y,delta);heading(g,null);
        paragraph(g,Component.translatable("wigo.settings.reset.confirm",Component.translatable(tab==WigoSettingsScreen.Tab.MINE?"wigo.settings.tab.mine":"wigo.settings.tab.others")),42,TEXT);
        if(tab==WigoSettingsScreen.Tab.MINE)paragraph(g,Component.translatable("wigo.settings.reset.sharing"),82,SECONDARY);
    }
}
