package dev.wigo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

/** Preserve the world view transform while drawing names after translucent terrain. */
public final class NameTagLayers {
    private static final ByteBufferBuilder storage=new ByteBufferBuilder(8192);
    private static final MultiBufferSource.BufferSource buffer=MultiBufferSource.immediate(storage);
    private static Matrix4f view;
    public static MultiBufferSource buffer() {
        if(view==null)view=new Matrix4f(RenderSystem.getModelViewMatrix());
        return buffer;
    }
    public static void renderClassic() {
        if(view==null){buffer.endBatch();return;}
        var stack=RenderSystem.getModelViewStack();stack.pushMatrix();stack.set(view);
        try {buffer.endBatch();}finally {stack.popMatrix();}
    }
    public static void endFrame() {view=null;}
}
