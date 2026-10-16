package dev.wigo.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonNull;
import com.mojang.serialization.JsonOps;
import dev.wigo.StackJson;
import net.minecraft.client.Minecraft;
import dev.wigo.client.ScreenRectangle;
import net.minecraft.resources.RegistryOps;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.DyeColor;
import dev.wigo.client.LegacyBannerPatterns;
import com.mojang.math.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class LegacyCapture {
    public static void entity(LivingEntity entity,int x0,int y0,int x1,int y1,float scale,float mouseX,float mouseY) {
        JsonObject preview=preview(entity);
        GuiCapture.model(GuiCapture.command("e",null,GuiCapture.clip(),entity.getId(),preview,GuiCapture.array(x0,y0,x1,y1),scale,mouseX,mouseY),
                new ScreenRectangle(x0,y0,x1-x0,y1-y0));
    }
    private static JsonObject preview(LivingEntity entity) {
        JsonObject preview=new JsonObject();
        if(entity.level.getEntity(entity.getId())!=entity) {
            preview.addProperty("type",Registry.ENTITY_TYPE.getKey(entity.getType()).toString());JsonObject equipment=new JsonObject();
            for(EquipmentSlot slot:EquipmentSlot.values()) {
                var stack=entity.getItemBySlot(slot);
                if(!stack.isEmpty())equipment.add(slot.getName(),StackJson.encode(stack,Minecraft.getInstance().level.registryAccess()));
            }
            preview.add("equipment",equipment);
            preview.add("angles",GuiCapture.array(entity.yBodyRot,entity.getXRot(),entity.getYRot(),entity.yHeadRot));
            if(entity instanceof net.minecraft.world.entity.decoration.ArmorStand stand)
                preview.add("stand",GuiCapture.array(stand.isShowArms(),stand.isNoBasePlate()));
        }
        return preview;
    }
    public static void banner(Matrix4f pose,net.minecraft.client.model.geom.ModelPart flag,DyeColor color,LegacyBannerPatterns patterns) {
        var j=LegacyMatrices.joml(pose);float scale=(float)Math.hypot(j.m00(),j.m01()),x=j.m30(),y=j.m31();
        // Loom moves the native flag relative to its usual block model. Use
        // that part's actual origin instead of assuming the block's -32 Y.
        int x0=Math.round(x+scale*(flag.x-10)/16f),x1=Math.round(x+scale*(flag.x+10)/16f);
        int y0=Math.round(y+scale*flag.y/16f),y1=Math.round(y+scale*(flag.y+40)/16f);
        var encoded=patterns.encode();
        GuiCapture.model(GuiCapture.command("n",null,GuiCapture.clip(),color.getId(),encoded,GuiCapture.array(x0,y0,x1,y1)),new ScreenRectangle(x0,y0,x1-x0,y1-y0));
    }
}
