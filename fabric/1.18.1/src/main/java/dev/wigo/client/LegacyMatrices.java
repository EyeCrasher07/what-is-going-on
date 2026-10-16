package dev.wigo.client;

import com.mojang.math.Matrix4f;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;

/** Convert only presentation transforms, with a reused buffer on Minecraft's render thread. */
public final class LegacyMatrices {
    private static final FloatBuffer BUFFER = BufferUtils.createFloatBuffer(16);
    public static org.joml.Matrix4f joml(Matrix4f matrix) {
        BUFFER.clear();
        matrix.store(BUFFER);
        return new org.joml.Matrix4f().set(BUFFER);
    }
    public static Matrix4f affine(float m00, float m01, float m10, float m11, float x, float y) {
        BUFFER.clear();
        new org.joml.Matrix4f().m00(m00).m01(m01).m10(m10).m11(m11).m30(x).m31(y).get(BUFFER);
        Matrix4f result = new Matrix4f();
        result.load(BUFFER);
        return result;
    }
}
