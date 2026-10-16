package dev.wigo.mixin;

import net.minecraft.client.renderer.rendertype.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderType.class)
public interface RenderTypeInvoker {
    @Invoker("create") static RenderType wigoCreate(String name, RenderSetup setup) { throw new AssertionError(); }
}
