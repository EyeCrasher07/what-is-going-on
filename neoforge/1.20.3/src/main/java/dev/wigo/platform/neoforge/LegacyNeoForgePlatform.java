package dev.wigo.platform.neoforge;
import dev.wigo.platform.Platform;
import dev.wigo.protocol.Wire;
import net.minecraft.client.Minecraft;
import net.minecraft.network.*;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.common.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.event.EventNetworkChannel;
import net.neoforged.neoforge.network.custom.payload.SimplePayload;
import io.netty.buffer.Unpooled;
import java.nio.file.Path;
import java.util.function.Consumer;
public final class LegacyNeoForgePlatform implements Platform.Bindings {
    private static Consumer<String> receiver=text->{};
    private static EventNetworkChannel activity,gui,state;
    private static EventNetworkChannel channel(net.minecraft.resources.ResourceLocation id) {
        var version=Integer.toString(Wire.VERSION);var accepts=NetworkRegistry.acceptMissingOr(version);
        return NetworkRegistry.newEventChannel(id,()->version,accepts,accepts);
    }
    public static void initialize() {
        activity=channel(Wire.Activity.TYPE.id());gui=channel(Wire.Gui.TYPE.id());state=channel(Wire.State.TYPE.id());
        activity.addListener((NetworkEvent.ClientCustomPayloadEvent event)->{
            var context=event.getSource();var text=Wire.Activity.CODEC.decode(event.getPayload()).json();context.setPacketHandled(true);
            if(context.getSender()!=null)context.enqueueWork(()->dev.wigo.WigoServer.activity(context.getSender(),text));
        });
        gui.addListener((NetworkEvent.ClientCustomPayloadEvent event)->{
            var context=event.getSource();var text=Wire.Gui.CODEC.decode(event.getPayload()).json();context.setPacketHandled(true);
            if(context.getSender()!=null)context.enqueueWork(()->dev.wigo.WigoServer.gui(context.getSender(),text));
        });
        state.addListener((NetworkEvent.ServerCustomPayloadEvent event)->{
            var context=event.getSource();var text=Wire.State.CODEC.decode(event.getPayload()).json();context.setPacketHandled(true);
            context.enqueueWork(()->receiver.accept(text));
        });
    }
    public static void clientReceiver(Consumer<String> value) {receiver=value;}
    private static Connection connection() {var listener=Minecraft.getInstance().getConnection();return listener==null?null:listener.getConnection();}
    public boolean canSend(ServerPlayer player) {return state.isRemotePresent(player.connection.connection);}
    public void send(ServerPlayer player,Wire.State payload) {var connection=player.connection.connection;player.connection.send(packet(payload,threshold(connection),PacketFlow.CLIENTBOUND));}
    public boolean canSendActivity() {var connection=connection();return connection!=null&&activity.isRemotePresent(connection);}
    public boolean canSendGui() {var connection=connection();return connection!=null&&gui.isRemotePresent(connection);}
    public void send(Wire.Activity payload) {var connection=connection();connection.send(packet(payload,threshold(connection),PacketFlow.SERVERBOUND));}
    public void send(Wire.Gui payload) {var connection=connection();connection.send(packet(payload,threshold(connection),PacketFlow.SERVERBOUND));}
    private static int threshold(Connection connection) {var encoder=connection.channel().pipeline().get(CompressionEncoder.class);return encoder==null?-1:encoder.getThreshold();}
    public static Packet<?> packet(CustomPacketPayload payload,int threshold,PacketFlow flow) {
        var raw=flow==PacketFlow.CLIENTBOUND?new ClientboundCustomPayloadPacket(payload):new ServerboundCustomPayloadPacket(payload);
        var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try(var scope=dev.wigo.protocol.CompressedText.minecraftCompression(threshold)) {
            buffer.writeVarInt(ConnectionProtocol.PLAY.codec(flow).packetId(raw));buffer.writeResourceLocation(payload.id());buffer.writeVarInt(0);int start=buffer.writerIndex();payload.write(buffer);
            buffer.readerIndex(start);var data=new FriendlyByteBuf(Unpooled.buffer(buffer.readableBytes()));data.writeBytes(buffer);
            var encoded=new SimplePayload(data,payload.id(),0);
            return flow==PacketFlow.CLIENTBOUND?new ClientboundCustomPayloadPacket(encoded):new ServerboundCustomPayloadPacket(encoded);
        } finally {buffer.release();}
    }
    public Path configDirectory() {return FMLPaths.CONFIGDIR.get();}
    public boolean isModLoaded(String id) {return ModList.get().isLoaded(id);}
}
