package dev.wigo.platform.fabric;

import dev.wigo.Wigo;
import dev.wigo.WigoServer;
import dev.wigo.platform.Platform;
import dev.wigo.protocol.Wire;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class WigoFabric implements ModInitializer {
    @Override public void onInitialize() {
        Platform.install(new FabricPlatform());
        PayloadTypeRegistry.playC2S().register(Wire.Activity.TYPE,Wire.Activity.CODEC);
        PayloadTypeRegistry.playC2S().register(Wire.Gui.TYPE,Wire.Gui.CODEC);
        PayloadTypeRegistry.playS2C().register(Wire.State.TYPE,Wire.State.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Wire.Activity.TYPE,(payload,context)->WigoServer.activity(context.player(),payload.json()));
        ServerPlayNetworking.registerGlobalReceiver(Wire.Gui.TYPE,(payload,context)->WigoServer.gui(context.player(),payload.json()));
        ServerTickEvents.END_SERVER_TICK.register(WigoServer::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->WigoServer.disconnect(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server->WigoServer.clear());
        Wigo.initialize();
    }
}
