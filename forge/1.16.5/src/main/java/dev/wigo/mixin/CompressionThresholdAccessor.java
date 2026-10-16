package dev.wigo.mixin;

import net.minecraft.network.NettyCompressionEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NettyCompressionEncoder.class)
public interface CompressionThresholdAccessor {
    @Accessor("threshold") int wigoThreshold();
}
