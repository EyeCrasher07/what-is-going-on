package dev.wigo.platform.forge;
import dev.wigo.platform.Platform;
import dev.wigo.protocol.Wire;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.CompressionEncoder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.EventNetworkChannel;
import net.minecraftforge.event.network.CustomPayloadEvent;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Old Forge needs raw payload buffers, including for its in-memory connections. */
public final class ForgePlatform implements Platform.Bindings {
    private static Consumer<String> receiver=text->{};
    private static EventNetworkChannel activity,gui,state;
    public static void initialize() {
        activity=ChannelBuilder.named(Wire.Activity.TYPE.id()).networkProtocolVersion(Wire.VERSION).optional().eventNetworkChannel();
        gui=ChannelBuilder.named(Wire.Gui.TYPE.id()).networkProtocolVersion(Wire.VERSION).optional().eventNetworkChannel();
        state=ChannelBuilder.named(Wire.State.TYPE.id()).networkProtocolVersion(Wire.VERSION).optional().eventNetworkChannel();
        activity.addListener((CustomPayloadEvent event)->{
            var context=event.getSource();var text=Wire.Activity.CODEC.decode(event.getPayload()).json();context.setPacketHandled(true);
            if(context.isServerSide()&&context.getSender()!=null)context.enqueueWork(()->dev.wigo.WigoServer.activity(context.getSender(),text));
        });
        gui.addListener((CustomPayloadEvent event)->{
            var context=event.getSource();var text=Wire.Gui.CODEC.decode(event.getPayload()).json();context.setPacketHandled(true);
            if(context.isServerSide()&&context.getSender()!=null)context.enqueueWork(()->dev.wigo.WigoServer.gui(context.getSender(),text));
        });
        state.addListener((CustomPayloadEvent event)->{
            var context=event.getSource();var text=Wire.State.CODEC.decode(event.getPayload()).json();context.setPacketHandled(true);
            if(context.isClientSide())context.enqueueWork(()->receiver.accept(text));
        });
    }
    public static void clientReceiver(Consumer<String> value) {receiver=value;}
    private static Connection connection() {var listener=Minecraft.getInstance().getConnection();return listener==null?null:listener.getConnection();}
    public boolean canSend(ServerPlayer player) {return state.isRemotePresent(player.connection.getConnection());}
    public void send(ServerPlayer player,Wire.State payload) {var connection=player.connection.getConnection();player.connection.send(packet(payload,threshold(connection),PacketFlow.CLIENTBOUND));}
    public boolean canSendActivity() {var connection=connection();return connection!=null&&activity.isRemotePresent(connection);}
    public boolean canSendGui() {var connection=connection();return connection!=null&&gui.isRemotePresent(connection);}
    public void send(Wire.Activity payload) {var connection=connection();connection.send(packet(payload,threshold(connection),PacketFlow.SERVERBOUND));}
    public void send(Wire.Gui payload) {var connection=connection();connection.send(packet(payload,threshold(connection),PacketFlow.SERVERBOUND));}
    private static int threshold(Connection connection) {
        var encoder=connection.channel().pipeline().get(CompressionEncoder.class);return encoder==null?-1:encoder.getThreshold();
    }
    public static Packet<?> packet(CustomPacketPayload payload,int threshold,PacketFlow flow) {
        var raw=flow==PacketFlow.CLIENTBOUND?new ClientboundCustomPayloadPacket(payload):new ServerboundCustomPayloadPacket(payload);
        var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try(var scope=dev.wigo.protocol.CompressedText.minecraftCompression(threshold)) {
            buffer.writeVarInt(ConnectionProtocol.PLAY.codec(flow).packetId(raw));buffer.writeResourceLocation(payload.id());int start=buffer.writerIndex();
            payload.write(buffer);
            // Include the actual packet header in the compression decision, then
            // give Forge just the body. Each observer owns its outgoing buffer.
            buffer.readerIndex(start);var data=new FriendlyByteBuf(Unpooled.buffer(buffer.readableBytes()));data.writeBytes(buffer);
            var encoded=new DiscardedPayload(payload.id(),data);
            return flow==PacketFlow.CLIENTBOUND?new ClientboundCustomPayloadPacket(encoded):new ServerboundCustomPayloadPacket(encoded);
        } finally {buffer.release();}
    }
    public Path configDirectory() {return FMLPaths.CONFIGDIR.get();}
    public boolean isModLoaded(String id) {return ModList.get().isLoaded(id);}
}
