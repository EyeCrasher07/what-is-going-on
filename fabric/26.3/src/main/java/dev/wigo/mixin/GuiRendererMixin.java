package dev.wigo.mixin;

import dev.wigo.client.OffscreenGui;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.WindowRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin implements OffscreenGui {
    @Unique private RenderTarget wigo$target;
    @Unique private final WindowRenderState wigo$window = new WindowRenderState();
    @Override public void wigoTarget(RenderTarget target, int scale) {
        wigo$target = target;
        if (target != null) { wigo$window.width = target.width; wigo$window.height = target.height; wigo$window.guiScale = scale; }
    }
    @Redirect(method = {"draw", "enableScissor", "getGuiScaleInvalidatingItemAtlasIfChanged", "preparePictureInPicture"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/GameRenderState;windowRenderState:Lnet/minecraft/client/renderer/state/WindowRenderState;"))
    private WindowRenderState wigoWindow(GameRenderState state) {
        return wigo$target != null ? wigo$window : state.windowRenderState;
    }
    @Redirect(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    private RenderTarget wigoRenderTarget(GameRenderer renderer) {
        return wigo$target != null ? wigo$target : renderer.mainRenderTarget();
    }
}
