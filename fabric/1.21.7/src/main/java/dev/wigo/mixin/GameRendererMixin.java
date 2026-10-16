package dev.wigo.mixin;

import dev.wigo.client.PanelRenderer;
import dev.wigo.client.WigoClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void wigoPrepare(CallbackInfo ci) {
        PanelRenderer.prepare();
    }
    @Inject(method = "close", at = @At("HEAD"))
    private void wigoClose(CallbackInfo ci) { WigoClient.reset(); }
}
