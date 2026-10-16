package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.matrix.MatrixStack;
import dev.wigo.client.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method="renderLevel", at=@At("HEAD"))
    private void prepare(CallbackInfo ci) { PanelRenderer.prepare(); }
    @Inject(method="close", at=@At("HEAD"))
    private void close(CallbackInfo ci) { WigoClient.reset(); }
}
