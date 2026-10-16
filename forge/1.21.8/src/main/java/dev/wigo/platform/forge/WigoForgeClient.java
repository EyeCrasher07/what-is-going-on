package dev.wigo.platform.forge;

import dev.wigo.client.GuiRendererFactory;
import dev.wigo.client.WigoClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import java.util.function.Supplier;

public final class WigoForgeClient {
    public static void register(net.minecraftforge.eventbus.api.bus.BusGroup modBus) {
        RegisterKeyMappingsEvent.getBus(modBus).addListener(WigoForgeClient::keys);
        TickEvent.ClientTickEvent.Post.BUS.addListener(WigoForgeClient::tick);
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(WigoForgeClient::joined);
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(WigoForgeClient::left);
    }
    private static void keys(RegisterKeyMappingsEvent event) {
        GuiRendererFactory.install((state,mc,factories)->new GuiRenderer(state,mc.renderBuffers().bufferSource(),factories.stream().map(Supplier::get).toList()));
        WigoClient.initialize();
        event.register(WigoClient.settingsKey());
        ForgePlatform.clientReceiver(WigoClient::receive);
    }
    private static void tick(TickEvent.ClientTickEvent.Post event) {
        WigoClient.tick(Minecraft.getInstance());
        if(Boolean.getBoolean("wigo.port.smoke")) {
            try {Class.forName("dev.wigo.test.PortTestDriver").getMethod("tick").invoke(null);}
            catch(ReflectiveOperationException failure) {throw new IllegalStateException("WIGO smoke driver missing",failure);}
        }
    }
    private static void joined(ClientPlayerNetworkEvent.LoggingIn event) { WigoClient.joined(); }
    private static void left(ClientPlayerNetworkEvent.LoggingOut event) { WigoClient.disconnected(); }
}
