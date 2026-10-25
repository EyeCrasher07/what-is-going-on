package dev.wigo.mixin;
import java.util.*;
import java.util.function.Supplier;
import dev.wigo.client.SettingsPreviewRenderer;
import dev.wigo.client.SettingsPreviewState;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import dev.wigo.client.OffscreenGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Apply the panel viewport to its own renderer without mutating Minecraft's window. */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin implements OffscreenGui {
    @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true)
    private static List<?> wigoPreviewRenderer(List<?> original) {
        var renderers=new ArrayList<Object>(original);
        // NeoForge's constructor takes lazy registrations; vanilla and Forge take instances.
        // This hook runs before client setup, so it cannot depend on installed loader bindings.
        try {
            var registration=Class.forName("net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration");
            renderers.add(registration.getConstructor(Class.class,java.util.function.Function.class).newInstance(
                    SettingsPreviewState.class,(java.util.function.Function<Object,SettingsPreviewRenderer>)(ignored->new SettingsPreviewRenderer())));
        } catch(ClassNotFoundException vanilla) {
            renderers.add(new SettingsPreviewRenderer());
        } catch(ReflectiveOperationException failure) {
            throw new IllegalStateException("WIGO preview renderer registration",failure);
        }
        return renderers;
    }
    @Unique private RenderTarget wigo$target;
    @Unique private int wigo$scale;

    @Override public void wigoTarget(RenderTarget target,int scale) {
        wigo$target=target;wigo$scale=scale;
    }
    @Redirect(method="draw",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getWidth()I"))
    private int panelWidth(Window window) {return wigo$target==null?window.getWidth():wigo$target.width;}
    @Redirect(method={"draw","enableScissor"},at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getHeight()I"))
    private int panelHeight(Window window) {return wigo$target==null?window.getHeight():wigo$target.height;}
    @Redirect(method={"draw","enableScissor","getGuiScaleInvalidatingItemAtlasIfChanged","preparePictureInPicture"},
            at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getGuiScale()I"))
    private int panelScale(Window window) {return wigo$target==null?window.getGuiScale():wigo$scale;}
    @Redirect(method="draw",at=@At(value="INVOKE",target="Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    private RenderTarget panelTarget(Minecraft mc) {return wigo$target==null?mc.getMainRenderTarget():wigo$target;}
}
