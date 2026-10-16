package dev.wigo.platform.neoforge;

import dev.wigo.Wigo;
import dev.wigo.WigoServer;
import dev.wigo.platform.Platform;
import dev.wigo.protocol.Wire;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod("wigo")
public final class WigoNeoForge {
    public WigoNeoForge(IEventBus modBus) {
        Platform.install(new NeoForgePlatform());
        modBus.addListener(WigoNeoForge::registerPayloads);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event)->WigoServer.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event)->{if(event.getEntity() instanceof ServerPlayer player) WigoServer.disconnect(player);});
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event)->WigoServer.clear());
        Wigo.initialize();
    }
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar=event.registrar(Integer.toString(Wire.VERSION)).optional();
        registrar.playToServer(Wire.Activity.TYPE,Wire.Activity.CODEC,(payload,context)->WigoServer.activity((ServerPlayer)context.player(),payload.json()));
        registrar.playToServer(Wire.Gui.TYPE,Wire.Gui.CODEC,(payload,context)->WigoServer.gui((ServerPlayer)context.player(),payload.json()));
        registrar.playToClient(Wire.State.TYPE,Wire.State.CODEC);
    }
}
