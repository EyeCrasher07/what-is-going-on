package dev.wigo.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.wigo.client.GuiScratch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Old render types explicitly bind the main framebuffer; scope them to WIGO. */
@Mixin(RenderTarget.class)
public abstract class GuiTargetMixin {
    @Inject(method="bindWrite",at=@At("HEAD"),cancellable=true)
    private void localTarget(boolean viewport,CallbackInfo ci) {
        RenderTarget own=GuiScratch.target();
        if(own!=null&&own!=(Object)this) {own.bindWrite(viewport);ci.cancel();}
    }
}
