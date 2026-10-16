package dev.wigo.mixin;

import dev.wigo.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InventoryScreen.class)
public abstract class EntityCaptureMixin {
    @Inject(method="renderEntityInInventoryFollowsMouse",at=@At("HEAD"),cancellable=true)
    private static void wigoEntity(GuiGraphics g,int x0,int y0,int x1,int y1,int size,float offset,float mouseX,float mouseY,LivingEntity entity,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->LegacyCapture.entity(entity,x0,y0,x1,y1,size,mouseX,mouseY));ci.cancel();}
    }
    @Inject(method="renderEntityInInventory",at=@At("HEAD"),cancellable=true)
    private static void wigoPreview(GuiGraphics g,float x,float y,int size,Vector3f translation,Quaternionf rotation,Quaternionf camera,LivingEntity entity,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->LegacyCapture.preview(entity,x,y,size,translation,rotation,camera));ci.cancel();}
    }
}
