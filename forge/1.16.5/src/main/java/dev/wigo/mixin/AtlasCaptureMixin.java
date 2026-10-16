package dev.wigo.mixin;

import dev.wigo.client.GuiCapture;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AtlasTexture.class)
public abstract class AtlasCaptureMixin {
    @Inject(method="getSprite",at=@At("RETURN"))
    private void localSprite(ResourceLocation id,CallbackInfoReturnable<TextureAtlasSprite> result) {
        GuiCapture.resolvedSprite(result.getReturnValue());
    }
}
