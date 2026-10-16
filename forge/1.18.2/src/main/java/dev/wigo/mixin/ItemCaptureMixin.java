package dev.wigo.mixin;

import dev.wigo.client.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemCaptureMixin {
    @Inject(method="tryRenderGuiItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;IIII)V", at=@At("HEAD"), cancellable=true)
    private void item(LivingEntity actor, ItemStack stack, int x, int y, int seed, int depth, CallbackInfo ci) {
        if (GuiCapture.recording()) {
            GuiCapture.record(() -> GuiCapture.item(GuiGraphics.wrap(new PoseStack()), stack, x, y, seed));
            ci.cancel();
        }
    }
}
