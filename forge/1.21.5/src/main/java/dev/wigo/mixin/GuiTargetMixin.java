package dev.wigo.mixin;

import dev.wigo.client.GuiScratch;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderStateShard.OutputStateShard.class)
public abstract class GuiTargetMixin {
    @Inject(method="getRenderTarget",at=@At("HEAD"),cancellable=true)
    private void wigoTarget(CallbackInfoReturnable<RenderTarget> ci) {
        if(GuiScratch.target()!=null)ci.setReturnValue(GuiScratch.target());
    }
}
