package dev.wigo.mixin;

import dev.wigo.client.OffscreenGui;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.WindowRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import java.util.ArrayList;
import java.util.List;
import dev.wigo.client.SettingsPreviewRenderer;
import dev.wigo.client.SettingsPreviewState;
import java.util.function.Supplier;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin implements OffscreenGui {
    @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true)
    private static List<?> wigoPreviewRenderer(List<?> original) {
        var renderers=new ArrayList<Object>(original);
        // NeoForge's constructor takes lazy registrations; vanilla and Forge take instances.
        // This hook runs before client setup, so it cannot depend on installed loader bindings.
        try {
            var registration=Class.forName("net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration");
            renderers.add(registration.getConstructor(Class.class,Supplier.class).newInstance(
                    SettingsPreviewState.class,(Supplier<SettingsPreviewRenderer>)SettingsPreviewRenderer::new));
        } catch(ClassNotFoundException vanilla) {
            renderers.add(new SettingsPreviewRenderer());
        } catch(ReflectiveOperationException failure) {
            throw new IllegalStateException("WIGO preview renderer registration",failure);
        }
        return renderers;
    }
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
