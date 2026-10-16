package dev.wigo.platform.forge;

import dev.wigo.platform.Platform;
import dev.wigo.protocol.Wire;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class ForgePlatform implements Platform.Bindings {
    private static Consumer<String> receiver = text -> {};
    private static Channel<CustomPacketPayload> channel;
    public static void initialize() {
        var play = ChannelBuilder.named("wigo:state_v"+Wire.VERSION).networkProtocolVersion(Wire.VERSION).optional().payloadChannel().play();
        play.serverbound().addMain(Wire.Activity.TYPE, Wire.Activity.CODEC, (payload,context) -> {
            if (context.getSender()!=null) dev.wigo.WigoServer.activity(context.getSender(),payload.json());
        });
        play.serverbound().addMain(Wire.Gui.TYPE, Wire.Gui.CODEC, (payload,context) -> {
            if (context.getSender()!=null) dev.wigo.WigoServer.gui(context.getSender(),payload.json());
        });
        channel = play.clientbound().addMain(Wire.State.TYPE,Wire.State.CODEC,(payload,context)->receiver.accept(payload.json())).build();
    }
    public static void clientReceiver(Consumer<String> value) { receiver=value; }
    private static Connection connection() {
        var listener=Minecraft.getInstance().getConnection();
        return listener==null?null:listener.getConnection();
    }
    @Override public boolean canSend(ServerPlayer player) { return channel.isRemotePresent(player.connection.getConnection()); }
    @Override public void send(ServerPlayer player,Wire.State payload) { channel.send(payload,player.connection.getConnection()); }
    @Override public boolean canSendActivity() { var connection=connection(); return connection!=null&&channel.isRemotePresent(connection); }
    @Override public boolean canSendGui() { return canSendActivity(); }
    @Override public void send(Wire.Activity payload) { channel.send(payload,connection()); }
    @Override public void send(Wire.Gui payload) { channel.send(payload,connection()); }
    @Override public Path configDirectory() { return FMLPaths.CONFIGDIR.get(); }
    @Override public boolean isModLoaded(String id) { return ModList.isLoaded(id); }
}
