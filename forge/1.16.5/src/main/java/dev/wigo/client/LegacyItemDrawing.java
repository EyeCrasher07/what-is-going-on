package dev.wigo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;

public final class LegacyItemDrawing {
    public static void draw(MatrixStack pose,Runnable draw) {
        RenderSystem.pushMatrix();
        try {RenderSystem.multMatrix(pose.last().pose());draw.run();}
        finally {RenderSystem.popMatrix();}
    }
}
