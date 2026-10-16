package dev.wigo.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.joml.Matrix4fc;
import org.joml.Matrix4f;
import java.util.*;

/** Name layers of panel owners follow water and precede transparent panels. */
public final class NameTagLayers {
    private static final Set<Matrix4fc> marked=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final ByteBufferBuilder storage=new ByteBufferBuilder(8192);
    private static final MultiBufferSource.BufferSource buffer=MultiBufferSource.immediate(storage);
    private static Matrix4f view;
    public static void mark(SubmitNodeStorage.NameTagSubmit name) {marked.add(name.pose());}
    public static boolean contains(Matrix4fc pose) {return marked.contains(pose);}
    public static MultiBufferSource buffer() {
        if(view==null)view=new Matrix4f(RenderSystem.getModelViewMatrix());
        return buffer;
    }
    public static void renderClassic() {
        if(view==null){buffer.endBatch();return;}
        var stack=RenderSystem.getModelViewStack();stack.pushMatrix();stack.set(view);
        try {buffer.endBatch();}finally {stack.popMatrix();}
    }
    public static void endFrame() {marked.clear();view=null;}
}
