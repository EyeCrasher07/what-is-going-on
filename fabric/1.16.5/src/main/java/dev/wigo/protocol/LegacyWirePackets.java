package dev.wigo.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.network.*;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;

/** Pack bounded WIGO frames once, with the actual Minecraft header and compression threshold. */
public final class LegacyWirePackets {
    private LegacyWirePackets() {}
    public static int threshold(Connection connection) {
        var encoder=((dev.wigo.mixin.ConnectionChannelAccessor)connection).wigoChannel().pipeline().get(CompressionEncoder.class);
        return encoder==null?-1:((dev.wigo.mixin.CompressionThresholdAccessor)encoder).wigoThreshold();
    }
    public static Packet<?> packet(LegacyPayload payload,int threshold,PacketFlow flow) {
        var empty=new FriendlyByteBuf(Unpooled.buffer(0));
        Packet<?> raw=flow==PacketFlow.CLIENTBOUND?new ClientboundCustomPayloadPacket(payload.id(),empty):new ServerboundCustomPayloadPacket(payload.id(),empty);
        var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try(var scope=CompressedText.minecraftCompression(threshold)) {
            buffer.writeVarInt(ConnectionProtocol.PLAY.getPacketId(flow,raw));buffer.writeResourceLocation(payload.id());
            int start=buffer.writerIndex();payload.write(buffer);buffer.readerIndex(start);
            var data=new FriendlyByteBuf(Unpooled.buffer(buffer.readableBytes()));data.writeBytes(buffer);
            return flow==PacketFlow.CLIENTBOUND?new ClientboundCustomPayloadPacket(payload.id(),data):new ServerboundCustomPayloadPacket(payload.id(),data);
        } finally {buffer.release();empty.release();}
    }
}
