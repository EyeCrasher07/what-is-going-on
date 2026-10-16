package dev.wigo.mixin;

import dev.wigo.client.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemCaptureMixin {
    @Inject(method="tryRenderGuiItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;II)V", at=@At("HEAD"), cancellable=true)
    private void item(LivingEntity actor, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (GuiCapture.recording()) {
            GuiCapture.record(() -> GuiCapture.item(GuiGraphics.wrap(new MatrixStack()), stack, x, y, 0));
            ci.cancel();
        }
    }
}
