package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wigo.protocol.CompressedText;
import dev.wigo.protocol.Wire;
import dev.wigo.protocol.NetworkSessions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.CompressionEncoder;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PacketEncoder.class)
public abstract class PacketEncoderMixin {
    @WrapMethod(method="encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V")
    private void wigoCompression(ChannelHandlerContext context,Packet<?> packet,ByteBuf buf,Operation<Void> original) {
        CustomPacketPayload payload=packet instanceof ClientboundCustomPayloadPacket clientbound?clientbound.payload()
                :packet instanceof ServerboundCustomPayloadPacket serverbound?serverbound.payload():null;
        // Forge wraps custom payloads before encoding, retaining their registered type.
        if (payload==null || !(payload.id().equals(Wire.State.TYPE.id())
                || payload.id().equals(Wire.Activity.TYPE.id()) || payload.id().equals(Wire.Gui.TYPE.id()))) {
            original.call(context,packet,buf);return;
        }
        CompressionEncoder compression=context.pipeline().get(CompressionEncoder.class);
        try(var scope=CompressedText.minecraftCompression(compression==null?-1:compression.getThreshold());
                var network=NetworkSessions.scope(NetworkSessions.peer(context.channel()))) {
            original.call(context,packet,buf);
        }
    }
}
