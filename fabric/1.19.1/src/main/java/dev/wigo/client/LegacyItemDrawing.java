package dev.wigo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

/** Old ItemRenderer consumes the global model-view stack rather than a GUI PoseStack. */
public final class LegacyItemDrawing {
    public static void draw(PoseStack pose, Runnable draw) {
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try {
            modelView.mulPoseMatrix(pose.last().pose());
            RenderSystem.applyModelViewMatrix();
            draw.run();
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }
}
