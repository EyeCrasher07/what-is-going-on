package dev.wigo.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wigo.client.GuiCapture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(AbstractContainerScreen.class)
public abstract class CarriedCaptureMixin {
    @WrapMethod(method="renderFloatingItem")
    private void carried(GuiGraphics g,net.minecraft.world.item.ItemStack stack,int x,int y,String text,Operation<Void> original) {
        GuiCapture.muted++;
        try { original.call(g,stack,x,y,text); } finally { GuiCapture.muted--; }
    }
}
