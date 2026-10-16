package dev.wigo.platform.neoforge;

import dev.wigo.client.WigoClient;
import dev.wigo.protocol.Wire;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value="wigo",dist=Dist.CLIENT)
public final class WigoNeoForgeClient {
    public WigoNeoForgeClient(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent event)->{
            WigoClient.initialize();event.register(WigoClient.settingsKey());
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event)->{
            WigoClient.tick(Minecraft.getInstance());
            if(Boolean.getBoolean("wigo.port.smoke")) smokeTick();
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event)->WigoClient.joined());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event)->WigoClient.disconnected());
    }
    private static void smokeTick() {
        try {Class.forName("dev.wigo.test.PortTestDriver").getMethod("tick").invoke(null);}
        catch(ReflectiveOperationException failure) {throw new IllegalStateException("WIGO smoke driver missing",failure);}
    }
}
