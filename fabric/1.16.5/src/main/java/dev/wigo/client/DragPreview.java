package dev.wigo.client;

import com.google.gson.*;
import dev.wigo.mixin.ContainerScreenAccessor;
import dev.wigo.platform.Platform;
import dev.wigo.protocol.Wire;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Send only the selected slot numbers while the mouse button is held. */
public final class DragPreview {
    private static String last;
    private static long lastSession=-1;
    public static void reset() {last=null;lastSession=-1;}
    public static void tick(Screen screen,String kind,long session) {
        if(lastSession!=session) {last=null;lastSession=session;}
        if(!Settings.sharing()||!GuiCapture.wanted()||!Platform.get().canSendGui()) {last=null;return;}
        if(!(screen instanceof AbstractContainerScreen<?> container)||!(kind.equals("chest")||kind.equals("inventory"))) {last=null;return;}
        JsonObject packet=message(container,session);
        if((packet.getAsJsonArray("slots").size()==0)&&last==null)return;
        String text=packet.toString();
        if(!text.equals(last)) {dev.wigo.protocol.NetworkBatch.send(new Wire.Gui(text));last=text;}
    }
    public static JsonObject message(AbstractContainerScreen<?> screen,long session) {
        var view=(ContainerScreenAccessor)screen;
        var slots=new JsonArray();
        if(view.wigoQuickCrafting()&&!net.minecraft.client.Minecraft.getInstance().player.inventory.getCarried().isEmpty())
            view.wigoQuickCraftSlots().stream().mapToInt(slot->slot.index).sorted().limit(90).forEach(slots::add);
        var packet=new JsonObject();packet.addProperty("op","drag");packet.addProperty("session",session);
        packet.addProperty("window",screen.getMenu().containerId);packet.addProperty("type",view.wigoQuickCraftType());packet.add("slots",slots);
        return packet;
    }
}
