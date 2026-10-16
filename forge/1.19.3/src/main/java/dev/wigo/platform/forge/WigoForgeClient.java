package dev.wigo.platform.forge;

import dev.wigo.client.WigoClient;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import java.util.function.Supplier;

public final class WigoForgeClient {
    public static void register(net.minecraftforge.eventbus.api.IEventBus modBus) {
        modBus.addListener(WigoForgeClient::keys);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(WigoForgeClient::tick);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(WigoForgeClient::joined);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(WigoForgeClient::left);
    }
    private static void keys(RegisterKeyMappingsEvent event) {
        WigoClient.initialize();
        event.register(WigoClient.settingsKey());
        ForgePlatform.clientReceiver(WigoClient::receive);
    }
    private static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        WigoClient.tick(Minecraft.getInstance());
        if(Boolean.getBoolean("wigo.port.smoke")) {
            try {Class.forName("dev.wigo.test.PortTestDriver").getMethod("tick").invoke(null);}
            catch(ReflectiveOperationException failure) {throw new IllegalStateException("WIGO smoke driver missing",failure);}
        }
    }
    private static void joined(ClientPlayerNetworkEvent.LoggingIn event) { WigoClient.joined(); }
    private static void left(ClientPlayerNetworkEvent.LoggingOut event) { WigoClient.disconnected(); }
}
