package dev.wigo.platform.neoforge;
import dev.wigo.Wigo;
import dev.wigo.WigoServer;
import dev.wigo.platform.Platform;
import dev.wigo.protocol.Wire;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
@Mod("wigo")
public final class WigoNeoForge {
    public WigoNeoForge(IEventBus modBus) {
        Platform.install(new NeoForgePlatform());
        if(net.neoforged.fml.loading.FMLEnvironment.dist==net.neoforged.api.distmarker.Dist.CLIENT)new WigoNeoForgeClient(modBus);
        modBus.addListener(WigoNeoForge::registerPayloads);
        NeoForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event)->{if(event.phase==TickEvent.Phase.END)WigoServer.tick(event.getServer());});
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event)->{if(event.getEntity() instanceof ServerPlayer player)WigoServer.disconnect(player);});
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event)->WigoServer.clear());
        Wigo.initialize();
    }
    private static void registerPayloads(RegisterPayloadHandlerEvent event) {
        var registrar=event.registrar("wigo").versioned(Integer.toString(Wire.VERSION)).optional();
        registrar.play(Wire.Activity.TYPE.id(),Wire.Activity.CODEC::decode,handler->handler.server((payload,context)->context.workHandler().execute(()->context.player().filter(ServerPlayer.class::isInstance).ifPresent(player->WigoServer.activity((ServerPlayer)player,payload.json())))));
        registrar.play(Wire.Gui.TYPE.id(),Wire.Gui.CODEC::decode,handler->handler.server((payload,context)->context.workHandler().execute(()->context.player().filter(ServerPlayer.class::isInstance).ifPresent(player->WigoServer.gui((ServerPlayer)player,payload.json())))));
        registrar.play(Wire.State.TYPE.id(),Wire.State.CODEC::decode,handler->handler.client((payload,context)->context.workHandler().execute(()->dev.wigo.client.WigoClient.receive(payload.json()))));
    }
}
