package dev.wigo.mixin;

import com.mojang.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Matrix4f.class)
public interface LegacyMatrixAccessor {
    @Accessor("m00") void wigo00(float value);
    @Accessor("m01") void wigo01(float value);
    @Accessor("m03") void wigo03(float value);
    @Accessor("m10") void wigo10(float value);
    @Accessor("m11") void wigo11(float value);
    @Accessor("m13") void wigo13(float value);
}
