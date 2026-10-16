package dev.wigo.mixin;

import dev.wigo.client.*;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public abstract class TextureBindCaptureMixin {
    @Inject(method="bind",at=@At("HEAD")) private void texture(ResourceLocation id,CallbackInfo ci) {
        if(GuiCapture.recording())GuiGraphics.captureTexture=id;
    }
}
