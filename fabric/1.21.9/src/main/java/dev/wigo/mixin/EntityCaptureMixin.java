package dev.wigo.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wigo.client.GuiCapture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(InventoryScreen.class)
public abstract class EntityCaptureMixin {
    @WrapMethod(method="renderEntityInInventoryFollowsMouse")
    private static void entity(GuiGraphics g,int x0,int y0,int x1,int y1,int size,float offset,float mouseX,float mouseY,LivingEntity entity,Operation<Void> original) {
        LivingEntity previous = GuiCapture.inventoryEntity;
        GuiCapture.inventoryEntity=entity; GuiCapture.inventoryMouseX=mouseX; GuiCapture.inventoryMouseY=mouseY;
        try { original.call(g,x0,y0,x1,y1,size,offset,mouseX,mouseY,entity); } finally { GuiCapture.inventoryEntity=previous; }
    }
}
