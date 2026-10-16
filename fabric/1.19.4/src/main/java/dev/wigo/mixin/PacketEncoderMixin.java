package dev.wigo.mixin;

import net.minecraft.network.PacketEncoder;
import org.spongepowered.asm.mixin.Mixin;

/** These games require eager custom buffers; LegacyWirePackets already scopes WIGO compression. */
@Mixin(PacketEncoder.class)
public abstract class PacketEncoderMixin {}
