package dev.wigo.platform.neoforge;
import dev.wigo.Wigo;
import dev.wigo.WigoServer;
import dev.wigo.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
@Mod("wigo")
public final class WigoNeoForge {
    public WigoNeoForge() {
        Platform.install(new LegacyNeoForgePlatform());LegacyNeoForgePlatform.initialize();
        var modBus=net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        if(net.neoforged.fml.loading.FMLEnvironment.dist==net.neoforged.api.distmarker.Dist.CLIENT)new WigoNeoForgeClient(modBus);
        NeoForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event)->{if(event.phase==TickEvent.Phase.END)WigoServer.tick(event.getServer());});
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event)->{if(event.getEntity() instanceof ServerPlayer player)WigoServer.disconnect(player);});
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event)->WigoServer.clear());Wigo.initialize();
    }
}
