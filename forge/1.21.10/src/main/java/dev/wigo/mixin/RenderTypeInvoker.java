package dev.wigo.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderType.class)
public interface RenderTypeInvoker {
    @Invoker("create") static RenderType.CompositeRenderType wigoCreate(String name,int bufferSize,RenderPipeline pipeline,RenderType.CompositeState state) {
        throw new AssertionError();
    }
}
