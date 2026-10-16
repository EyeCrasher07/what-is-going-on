package dev.wigo.mixin;

import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderType.class)
public interface RenderTypeInvoker {
    @Invoker("create") static RenderType.Type wigoCreate(String name,VertexFormat format,int mode,int bufferSize,RenderType.State state) {
        throw new AssertionError();
    }
}
