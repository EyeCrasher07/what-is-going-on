package dev.wigo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import org.lwjgl.opengl.GL11;

/** Keep fixed-function names in the world view used when their vertices were collected. */
public final class NameTagLayers {
    private static final BufferBuilder storage=new BufferBuilder(8192);
    private static final MultiBufferSource.BufferSource buffer=MultiBufferSource.immediate(storage);
    private static float[] view;
    public static MultiBufferSource buffer() {
        if(view==null){view=new float[16];GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX,view);}
        return buffer;
    }
    public static void renderClassic() {
        if(view==null){buffer.endBatch();return;}
        int mode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);RenderSystem.pushMatrix();GL11.glLoadMatrixf(view);
        try {buffer.endBatch();}finally {RenderSystem.popMatrix();RenderSystem.matrixMode(mode);}
    }
    public static void endFrame(){view=null;}
}
