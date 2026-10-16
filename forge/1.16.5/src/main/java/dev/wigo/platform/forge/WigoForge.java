package dev.wigo.platform.forge;

import dev.wigo.Wigo;
import dev.wigo.WigoServer;
import dev.wigo.platform.Platform;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;

@Mod("wigo")
public final class WigoForge {
    public WigoForge() {
        var context=FMLJavaModLoadingContext.get();
        Platform.install(new ForgePlatform());
        ForgePlatform.initialize();
        if(FMLEnvironment.dist==Dist.CLIENT) WigoForgeClient.register(context.getModEventBus());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {if(event.phase==TickEvent.Phase.END){var server=net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();if(server!=null)WigoServer.tick(server);}});
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if(event.getEntity() instanceof ServerPlayerEntity player) WigoServer.disconnect(player);
        });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((FMLServerStoppedEvent event) -> WigoServer.clear());
        Wigo.initialize();
    }
}
