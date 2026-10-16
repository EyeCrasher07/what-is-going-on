package dev.wigo.mixin;

import dev.wigo.client.*;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class EntityCaptureMixin {
    @Inject(method="renderEntityInInventory", at=@At("HEAD"), cancellable=true)
    private static void entity(int x, int y, int size, float mouseX, float mouseY, LivingEntity entity, CallbackInfo ci) {
        if (GuiCapture.recording()) {
            GuiCapture.record(() -> LegacyCapture.entity(entity, x-25, y-66, x+25, y+3, size, x-mouseX, y-50-mouseY));
            ci.cancel();
        }
    }
}
