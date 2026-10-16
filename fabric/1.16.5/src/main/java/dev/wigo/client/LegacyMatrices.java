package dev.wigo.client;

import com.mojang.math.Matrix4f;
import dev.wigo.mixin.LegacyMatrixAccessor;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;

public final class LegacyMatrices {
    private static final FloatBuffer BUFFER=BufferUtils.createFloatBuffer(16);
    public static org.joml.Matrix4f joml(Matrix4f matrix) {
        BUFFER.clear();matrix.store(BUFFER);return new org.joml.Matrix4f().set(BUFFER);
    }
    public static Matrix4f affine(float m00,float m01,float m10,float m11,float x,float y) {
        Matrix4f result=new Matrix4f();result.setIdentity();
        var values=(LegacyMatrixAccessor)(Object)result;
        values.wigo00(m00);values.wigo01(m10);values.wigo03(x);
        values.wigo10(m01);values.wigo11(m11);values.wigo13(y);
        return result;
    }
}
