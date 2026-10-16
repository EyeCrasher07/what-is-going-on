package dev.wigo.mixin;
import dev.wigo.client.GuiCapture;
import net.minecraft.client.renderer.state.gui.*;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiRenderState.class)
public abstract class SceneStateMixin {
    @Inject(method="addText",at=@At("HEAD"))
    private void text(GuiTextRenderState state, CallbackInfo ci) { GuiCapture.record(()->GuiCapture.text(state)); }
    @Inject(method="addGuiElement",at=@At("HEAD"))
    private void rectangle(GuiElementRenderState state, CallbackInfo ci) { if (state instanceof ColoredRectangleRenderState r) GuiCapture.record(()->GuiCapture.rectangle(r)); }
    @Inject(method="addPicturesInPictureState",at=@At("HEAD"))
    private void pip(PictureInPictureRenderState state, CallbackInfo ci) { GuiCapture.record(()->GuiCapture.pip(state)); }
    @Inject(method="nextStratum",at=@At("HEAD"))
    private void layer(CallbackInfo ci) { GuiCapture.layer(); }
}
