package dev.wigo.client;

import dev.wigo.config.CursorStyle;
import dev.wigo.client.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/** The owner chooses the pointer shown on shared menus, without changing their OS cursor. */
public final class CursorSelectionScreen extends WigoScreen {
    private final List<Button> choices=new ArrayList<>();
    private Button done;
    public CursorSelectionScreen(WigoSettingsScreen previous) {super(previous,"wigo.settings.cursor.title");}
    @Override protected int preferredWidth() {return 360;}
    @Override protected int preferredHeight() {return 214;}
    @Override protected void init() {
        choices.clear();
        for(var style:CursorStyle.values()) {
            var choice=addRenderableWidget(WigoIconButton.cursorButton(new net.minecraft.network.chat.TranslatableComponent(style.label()),b->{
                Settings.cursorStyle=style;Settings.save();refreshSelection();
            },style));
            choices.add(choice);
        }
        done=button(new net.minecraft.network.chat.TranslatableComponent("gui.done"),this::onClose,null);
        refreshSelection();layoutWidgets();
    }
    private void refreshSelection() {
        for(int i=0;i<choices.size();i++)choices.get(i).active=CursorStyle.values()[i]!=Settings.cursorStyle;
    }
    @Override public void layoutWidgets() {
        int columns=2,gap=6,cell=(bodyWidth()-gap)/columns,step=Math.max(21,Math.min(30,(bodyHeight()-64)/4));
        for(int i=0;i<choices.size();i++)LegacyWidgetLayout.resize(choices.get(i),cell,20,
                bodyX()+(i%columns)*(cell+gap),bodyY()+34+(i/columns)*step);
        LegacyWidgetLayout.resize(done,bodyWidth(),20,bodyX(),bodyBottom()-26);
    }
    @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,int x,int y,float delta) {var g=GuiGraphics.wrap(pose);
        super.render(g.pose(),x,y,delta);heading(g,null);
    }
}
