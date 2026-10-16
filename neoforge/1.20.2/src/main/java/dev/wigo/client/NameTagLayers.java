package dev.wigo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

/** Preserve the world view transform while drawing names after translucent terrain. */
public final class NameTagLayers {
    private static final BufferBuilder storage=new BufferBuilder(8192);
    private static final MultiBufferSource.BufferSource buffer=MultiBufferSource.immediate(storage);
    private static Matrix4f view;
    public static MultiBufferSource buffer() {
        if(view==null)view=new Matrix4f(RenderSystem.getModelViewMatrix());
        return buffer;
    }
    public static void renderClassic() {
        if(view==null){buffer.endBatch();return;}
        var stack=RenderSystem.getModelViewStack();stack.pushPose();stack.last().pose().set(view);
        RenderSystem.applyModelViewMatrix();try {buffer.endBatch();}finally {stack.popPose();RenderSystem.applyModelViewMatrix();}
    }
    public static void endFrame() {view=null;}
}
