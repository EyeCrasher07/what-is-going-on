package dev.wigo.mixin;
import dev.wigo.client.GuiCapture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(RenderSystem.class)
public abstract class TextureBindCaptureMixin {
    @Inject(method="setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V",at=@At("HEAD"))
    private static void resource(int unit,ResourceLocation texture,CallbackInfo ci) {if(unit==0&&GuiCapture.recording())dev.wigo.client.GuiGraphics.captureTexture=texture;}
    @Inject(method="clear",at=@At("HEAD"),cancellable=true)
    private static void noWindowClear(int mask,boolean check,CallbackInfo ci) {if(GuiCapture.recording())ci.cancel();}
}
