package dev.wigo.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.network.NettyCompressionEncoder;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.network.play.client.CCustomPayloadPacket;

/** Pack bounded WIGO frames once, with the actual Minecraft header and compression threshold. */
public final class LegacyWirePackets {
    private LegacyWirePackets() {}
    public static int threshold(NetworkManager connection) {
        var encoder=((dev.wigo.mixin.ConnectionChannelAccessor)connection).wigoChannel().pipeline().get(NettyCompressionEncoder.class);
        return encoder==null?-1:((dev.wigo.mixin.CompressionThresholdAccessor)encoder).wigoThreshold();
    }
    public static IPacket<?> packet(LegacyPayload payload,int threshold,PacketDirection flow) {
        var empty=new PacketBuffer(Unpooled.buffer(0));
        IPacket<?> raw=flow==PacketDirection.CLIENTBOUND?new SCustomPayloadPlayPacket(payload.id(),empty):new CCustomPayloadPacket(payload.id(),empty);
        var buffer=new PacketBuffer(Unpooled.buffer());
        try(var scope=CompressedText.minecraftCompression(threshold)) {
            buffer.writeVarInt(ProtocolType.PLAY.getPacketId(flow,raw));buffer.writeResourceLocation(payload.id());
            int start=buffer.writerIndex();payload.write(buffer);buffer.readerIndex(start);
            var data=new PacketBuffer(Unpooled.buffer(buffer.readableBytes()));data.writeBytes(buffer);
            return flow==PacketDirection.CLIENTBOUND?new SCustomPayloadPlayPacket(payload.id(),data):new CCustomPayloadPacket(payload.id(),data);
        } finally {buffer.release();empty.release();}
    }
}
