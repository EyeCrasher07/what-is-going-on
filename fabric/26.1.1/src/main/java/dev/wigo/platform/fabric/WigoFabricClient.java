package dev.wigo.platform.fabric;

import dev.wigo.client.WigoClient;
import dev.wigo.client.GuiRendererFactory;
import net.minecraft.client.gui.render.GuiRenderer;
import java.util.function.Supplier;
import dev.wigo.protocol.Wire;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class WigoFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        GuiRendererFactory.install((state,mc,factories)->new GuiRenderer(state,mc.renderBuffers().bufferSource(),mc.gameRenderer.getSubmitNodeStorage(),mc.gameRenderer.getFeatureRenderDispatcher(),factories.stream().map(Supplier::get).toList()));
        WigoClient.initialize();
        KeyMappingHelper.registerKeyMapping(WigoClient.settingsKey());
        ClientPlayNetworking.registerGlobalReceiver(Wire.State.TYPE,(payload,context)->WigoClient.receive(payload.json()));
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->WigoClient.joined());
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->WigoClient.disconnected());
        ClientTickEvents.END_CLIENT_TICK.register(WigoClient::tick);
    }
}
