package dev.wigo.mixin;

import net.minecraft.network.CompressionEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompressionEncoder.class)
public interface CompressionThresholdAccessor {
    @Accessor("threshold") int wigoThreshold();
}
