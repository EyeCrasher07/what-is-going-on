package dev.wigo.mixin;
import dev.wigo.client.GuiCapture;
import net.minecraft.client.resources.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AtlasManager.class)
public abstract class AtlasCaptureMixin {
    @Inject(method="get",at=@At("RETURN"))
    private void localSprite(Material id,CallbackInfoReturnable<TextureAtlasSprite> result) {GuiCapture.resolvedSprite(result.getReturnValue());}
}
