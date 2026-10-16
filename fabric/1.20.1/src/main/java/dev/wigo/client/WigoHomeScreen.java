package dev.wigo.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** F8 entry point: global enable switch, existing settings and Info. */
public final class WigoHomeScreen extends WigoScreen {
    private Button enabled,settings,info,done;
    public WigoHomeScreen(Screen previous){super(previous,"wigo.home.title");}
    @Override protected int preferredWidth(){return 220;}
    @Override protected int preferredHeight(){return 142;}
    @Override protected void init(){
        enabled=button(toggle("wigo.home.enabled",Settings.enabled),()->{WigoClient.enabled(!Settings.enabled);enabled.setMessage(toggle("wigo.home.enabled",Settings.enabled));},"wigo.home.enabled.description");
        settings=addRenderableWidget(new WigoIconButton(Component.translatable("wigo.home.settings"),b->minecraft.setScreen(new WigoSettingsScreen(this)),null,true));
        info=addRenderableWidget(new WigoIconButton(Component.literal("Info"),b->minecraft.setScreen(new WigoInfoScreen(this)),new ResourceLocation("wigo","textures/gui/info.png"),false));
        done=button(Component.translatable("gui.done"),this::onClose,null);
        layoutWidgets();
    }
    @Override public void layoutWidgets(){
        int x=bodyX(),y=bodyY(),w=bodyWidth();
        LegacyWidgetLayout.resize(enabled,w,20,x,y+34);
        LegacyWidgetLayout.resize(settings,(w-6)/2,20,x,y+64);
        LegacyWidgetLayout.resize(info,(w-6)/2,20,x+(w+6)/2,y+64);
        LegacyWidgetLayout.resize(done,w,20,x,bodyBottom()-28);
    }
    @Override public void render(GuiGraphics g,int x,int y,float delta){super.render(g,x,y,delta);heading(g,null);}
}
