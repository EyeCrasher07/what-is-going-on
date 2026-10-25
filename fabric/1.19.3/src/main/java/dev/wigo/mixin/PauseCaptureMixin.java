package dev.wigo.mixin;
import dev.wigo.client.GuiCapture;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Isolate native contents; the surrounding Screen still draws native tooltips. */
@Mixin(PauseScreen.class)
public abstract class PauseCaptureMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void wigoContents(com.mojang.blaze3d.vertex.PoseStack graphics,int x,int y,float delta,CallbackInfo ci) {
        if(GuiCapture.active()&&GuiCapture.isolatedPause((PauseScreen)(Object)this)) {
            GuiCapture.pauseContents((PauseScreen)(Object)this,dev.wigo.client.GuiGraphics.wrap(graphics),x,y,delta);ci.cancel();
        }
    }
}
