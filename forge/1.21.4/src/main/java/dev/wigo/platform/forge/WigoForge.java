package dev.wigo.platform.forge;

import dev.wigo.Wigo;
import dev.wigo.WigoServer;
import dev.wigo.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;

@Mod("wigo")
public final class WigoForge {
    public WigoForge(FMLJavaModLoadingContext context) {
        Platform.install(new ForgePlatform());
        ForgePlatform.initialize();
        if(FMLEnvironment.dist==Dist.CLIENT) WigoForgeClient.register(context.getModEventBus());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent.Post event) -> WigoServer.tick(event.getServer()));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if(event.getEntity() instanceof ServerPlayer player) WigoServer.disconnect(player);
        });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> WigoServer.clear());
        Wigo.initialize();
    }
}
