package dev.wigo.client;

import dev.wigo.client.GuiGraphics;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;

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
        yes=button(new net.minecraft.util.text.TranslationTextComponent("gui.yes"),()->{
            settings.resetSettings();
            minecraft.setScreen(settings);
        },null);
        no=button(new net.minecraft.util.text.TranslationTextComponent("gui.no"),this::onClose,null);
        layoutWidgets();
    }
    @Override public void layoutWidgets(){
        int x=bodyX(),w=bodyWidth(),y=bodyBottom()-30;
        LegacyWidgetLayout.resize(yes,(w-6)/2,20,x,y);LegacyWidgetLayout.resize(no,(w-6)/2,20,x+(w+6)/2,y);
    }
    @Override public void render(com.mojang.blaze3d.matrix.MatrixStack pose,int x,int y,float delta) {var g=GuiGraphics.wrap(pose);
        super.render(g.pose(),x,y,delta);heading(g,null);
        paragraph(g,new net.minecraft.util.text.TranslationTextComponent("wigo.settings.reset.confirm",new net.minecraft.util.text.TranslationTextComponent(tab==WigoSettingsScreen.Tab.MINE?"wigo.settings.tab.mine":"wigo.settings.tab.others")),42,TEXT);
        if(tab==WigoSettingsScreen.Tab.MINE)paragraph(g,new net.minecraft.util.text.TranslationTextComponent("wigo.settings.reset.sharing"),82,SECONDARY);
    }
}
