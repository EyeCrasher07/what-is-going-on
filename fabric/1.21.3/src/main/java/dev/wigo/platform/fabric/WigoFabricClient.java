package dev.wigo.platform.fabric;

import dev.wigo.client.WigoClient;
import java.util.function.Supplier;
import dev.wigo.protocol.Wire;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class WigoFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        WigoClient.initialize();
        KeyBindingHelper.registerKeyBinding(WigoClient.settingsKey());
        ClientPlayNetworking.registerGlobalReceiver(Wire.State.TYPE,(payload,context)->WigoClient.receive(payload.json()));
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->WigoClient.joined());
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->WigoClient.disconnected());
        ClientTickEvents.END_CLIENT_TICK.register(WigoClient::tick);
    }
}
