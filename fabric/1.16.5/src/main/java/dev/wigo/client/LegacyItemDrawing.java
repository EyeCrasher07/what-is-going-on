package dev.wigo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

public final class LegacyItemDrawing {
    public static void draw(PoseStack pose,Runnable draw) {
        RenderSystem.pushMatrix();
        try {RenderSystem.multMatrix(pose.last().pose());draw.run();}
        finally {RenderSystem.popMatrix();}
    }
}
