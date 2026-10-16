package dev.wigo.client;

import dev.wigo.mixin.ChatAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

public final class ChatPreview {
    public static void draw(GuiGraphics g,ChatScreen screen) {
        Layout layout=layout(screen);
        Minecraft mc=Minecraft.getInstance();
        int y=layout.baseline();
        for (var line:layout.lines()) {g.drawString(mc.font,line,4,y,0xFFFFFFFF,true);y+=layout.step();}
    }

    public static int historyOffset(ChatScreen screen) {
        Layout layout=layout(screen);
        if (layout.lines().isEmpty()) return 0;
        // Leave room for the vanilla delayed-message queue below the newest message.
        int historyBottom=screen.height-40+(int)Math.ceil(9*Minecraft.getInstance().options.chatScale().get());
        return Math.max(0,historyBottom+4-layout.top());
    }

    private static Layout layout(ChatScreen screen) {
        Minecraft mc=Minecraft.getInstance();
        if (!Settings.receiving() || mc.player==null || mc.level==null) return new Layout(List.of(),0,0,1);
        var input=((ChatAccessor)screen).wigoInput();
        if (input==null) return new Layout(List.of(),0,0,1);
        int width=Math.max(1,Math.min(screen.width-8,ChatComponent.getWidth(mc.options.chatWidth().get())));
        List<FormattedCharSequence> lines=new ArrayList<>();
        for (RemoteMenu menu:WigoClient.menus.values()) {
            if (menu.scene==null || !menu.scene.ready) continue;
            var actor=mc.level.getPlayerByUUID(menu.owner);
            if (actor==null||actor.distanceToSqr(mc.player)>menu.state.distance*menu.state.distance) continue;
            if(PanelRenderer.cameraOpacity(menu,actor,mc.gameRenderer.getLevelRenderState().cameraRenderState)<=0)continue;
            for (var command:menu.scene.commands()) if (command.get(0).getAsString().equals("q")) {
                String draft=command.get(3).getAsString();
                if (!draft.isEmpty()) {
                    lines.addAll(mc.font.split(Component.literal(menu.state.name+": "+draft),width));
                }
            }
        }
        int minY=0,maxY=mc.font.lineHeight;
        for (var line:lines) {
            var bounds=mc.font.prepareText(line,0,0,0xFFFFFFFF,true,false,0).bounds();
            if (bounds!=null) {minY=Math.min(minY,bounds.top());maxY=Math.max(maxY,bounds.bottom());}
        }
        int bottom=input.getY()-4,step=maxY-minY+2;
        int available=Math.max(0,(bottom-12)/step);
        if (lines.size()>available) {
            lines=new ArrayList<>(lines.subList(lines.size()-available,lines.size()));
            if (!lines.isEmpty()) lines.set(0,Component.literal("…").getVisualOrderText());
        }
        int top=bottom-lines.size()*step;
        return new Layout(lines,top,top-minY,step);
    }

    private record Layout(List<FormattedCharSequence> lines,int top,int baseline,int step) {}
}
