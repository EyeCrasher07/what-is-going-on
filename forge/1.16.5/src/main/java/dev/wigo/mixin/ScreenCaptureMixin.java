package dev.wigo.mixin;

import dev.wigo.client.GuiCapture;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenCaptureMixin {
    @Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
    private void scenery(int offset, CallbackInfo ci) {
        if (GuiCapture.recording()) ci.cancel();
    }
}
