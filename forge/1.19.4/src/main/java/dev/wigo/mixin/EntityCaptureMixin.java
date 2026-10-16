package dev.wigo.mixin;

import dev.wigo.client.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class EntityCaptureMixin {
    @Inject(method="renderEntityInInventoryFollowsMouse",at=@At("HEAD"),cancellable=true)
    private static void wigoEntity(PoseStack g,int x,int y,int size,float mouseX,float mouseY,LivingEntity entity,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->LegacyCapture.entity(entity,x-25,y-66,x+25,y+3,size,x-mouseX,y-50-mouseY));ci.cancel();}
    }
    @Inject(method="renderEntityInInventory",at=@At("HEAD"),cancellable=true)
    private static void wigoPreview(PoseStack g,int x,int y,int size,Quaternionf rotation,Quaternionf camera,LivingEntity entity,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->LegacyCapture.preview(entity,x,y,size,new org.joml.Vector3f(),rotation,camera));ci.cancel();}
    }
}
