package dev.wigo.mixin;

import net.minecraft.client.shader.Framebuffer;
import dev.wigo.client.GuiScratch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Old render types explicitly bind the main framebuffer; scope them to WIGO. */
@Mixin(Framebuffer.class)
public abstract class GuiTargetMixin {
    @Inject(method="bindWrite",at=@At("HEAD"),cancellable=true)
    private void localTarget(boolean viewport,CallbackInfo ci) {
        Framebuffer own=GuiScratch.target();
        if(own!=null&&own!=(Object)this) {own.bindWrite(viewport);ci.cancel();}
    }
}
