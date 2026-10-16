package dev.wigo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;

/** Native GUI coordinates include the container's global model-view translation. */
public final class LegacyCapturePose {
    private LegacyCapturePose() {}
    public static Matrix4f combine(Matrix4f local) {
        return new Matrix4f(RenderSystem.getModelViewStack().last().pose()).mul(local);
    }
}
