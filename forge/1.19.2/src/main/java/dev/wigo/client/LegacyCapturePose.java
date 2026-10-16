package dev.wigo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;

/** Native GUI coordinates include the container's global model-view translation. */
public final class LegacyCapturePose {
    private LegacyCapturePose() {}
    public static Matrix4f combine(Matrix4f local) {
        Matrix4f result=RenderSystem.getModelViewStack().last().pose().copy();
        result.multiply(local);
        return result;
    }
}
