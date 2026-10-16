package dev.wigo.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wigo.client.GuiCapture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(AbstractContainerScreen.class)
public abstract class CarriedCaptureMixin {
    @WrapMethod(method="extractCarriedItem")
    private void carried(GuiGraphicsExtractor g,int x,int y,Operation<Void> original) {
        GuiCapture.muted++;
        try { original.call(g,x,y); } finally { GuiCapture.muted--; }
    }
}
