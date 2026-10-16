package dev.wigo.mixin;

import net.minecraft.network.NettyPacketEncoder;
import org.spongepowered.asm.mixin.Mixin;

/** These games require eager custom buffers; LegacyWirePackets already scopes WIGO compression. */
@Mixin(NettyPacketEncoder.class)
public abstract class PacketEncoderMixin {}
