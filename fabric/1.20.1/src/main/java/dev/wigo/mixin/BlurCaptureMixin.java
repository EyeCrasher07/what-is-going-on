package dev.wigo.mixin;
import dev.wigo.client.GuiCapture;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class BlurCaptureMixin {
    @Inject(method="processBlurEffect",at=@At("HEAD"),cancellable=true,require=0)
    private void wigoInstructions(CallbackInfo ci) {if(GuiCapture.recording())ci.cancel();}
}
