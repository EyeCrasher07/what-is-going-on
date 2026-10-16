package dev.wigo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

/** Preserve the world view transform while drawing names after translucent terrain. */
public final class NameTagLayers {
    private static final ByteBufferBuilder storage=new ByteBufferBuilder(8192);
    private static final MultiBufferSource.BufferSource batches=MultiBufferSource.immediate(storage);
    private static final java.util.Map<net.minecraft.client.renderer.RenderType,net.minecraft.client.renderer.RenderType> types=new java.util.IdentityHashMap<>();
    private static final MultiBufferSource buffer=type->batches.getBuffer(nameType(type));
    private static net.minecraft.client.renderer.RenderType nameType(net.minecraft.client.renderer.RenderType base) {
        if(types.size()>=128)types.clear();
        return types.computeIfAbsent(base,type->new net.minecraft.client.renderer.RenderType(
                "wigo_name",type.format(),type.mode(),type.bufferSize(),type.affectsCrumbling(),false,
                ()->{type.setupRenderState();RenderSystem.depthMask(false);},
                ()->{type.clearRenderState();RenderSystem.depthMask(true);}){});
    }
    private static Matrix4f view;
    public static MultiBufferSource buffer() {
        if(view==null)view=new Matrix4f(RenderSystem.getModelViewMatrix());
        return buffer;
    }
    public static void renderClassic() {
        if(view==null){batches.endBatch();return;}
        var stack=RenderSystem.getModelViewStack();stack.pushMatrix();stack.set(view);
        RenderSystem.applyModelViewMatrix();try {batches.endBatch();}finally {stack.popMatrix();RenderSystem.applyModelViewMatrix();}
    }
    public static void endFrame() {view=null;}
}
