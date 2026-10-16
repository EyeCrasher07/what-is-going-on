package dev.wigo.platform.fabric;

import dev.wigo.Wigo;
import dev.wigo.WigoServer;
import dev.wigo.platform.Platform;
import dev.wigo.protocol.Wire;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class WigoFabric implements ModInitializer {
    @Override public void onInitialize() {
        Platform.install(new FabricPlatform());
        ServerPlayNetworking.registerGlobalReceiver(LegacyFabricPackets.ACTIVITY,(packet,player,sender)->WigoServer.activity(player,((Wire.Activity)packet.payload()).json()));
        ServerPlayNetworking.registerGlobalReceiver(LegacyFabricPackets.GUI,(packet,player,sender)->WigoServer.gui(player,((Wire.Gui)packet.payload()).json()));
        ServerTickEvents.END_SERVER_TICK.register(WigoServer::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->WigoServer.disconnect(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server->WigoServer.clear());
        Wigo.initialize();
    }
}
