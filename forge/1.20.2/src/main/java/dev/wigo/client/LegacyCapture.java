package dev.wigo.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonNull;
import com.mojang.serialization.JsonOps;
import dev.wigo.StackJson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.resources.RegistryOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.DyeColor;
import dev.wigo.client.LegacyBannerPatterns;
import org.joml.Matrix4f;
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
        if(entity.level().getEntity(entity.getId())!=entity) {
            preview.addProperty("type",BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());JsonObject equipment=new JsonObject();
            for(EquipmentSlot slot:EquipmentSlot.values()) {
                var stack=entity.getItemBySlot(slot);
                if(!stack.isEmpty())equipment.add(slot.getSerializedName(),StackJson.encode(stack,Minecraft.getInstance().level.registryAccess()));
            }
            preview.add("equipment",equipment);
            preview.add("angles",GuiCapture.array(entity.yBodyRot,entity.getXRot(),entity.getYRot(),entity.yHeadRot));
            if(entity instanceof net.minecraft.world.entity.decoration.ArmorStand stand)
                preview.add("stand",GuiCapture.array(stand.isShowArms(),stand.isNoBasePlate()));
        }
        return preview;
    }
    public static void preview(LivingEntity entity,float x,float y,float scale,Vector3f translation,Quaternionf rotation,Quaternionf camera) {
        JsonObject preview=preview(entity);JsonArray transform=new JsonArray();
        transform.add(GuiCapture.array(translation.x,translation.y,translation.z));
        transform.add(GuiCapture.array(rotation.x,rotation.y,rotation.z,rotation.w));
        transform.add(camera==null?JsonNull.INSTANCE:GuiCapture.array(camera.x,camera.y,camera.z,camera.w));
        preview.add("transform",transform);
        float left=Float.POSITIVE_INFINITY,top=left,right=Float.NEGATIVE_INFINITY,bottom=right;
        // Project the native model volume through the original transform. The
        // origin is a render anchor, not a mouse-following inventory rectangle.
        for(int corner=0;corner<8;corner++) {
            float radius=Math.max(.5f,entity.getBbWidth());
            var point=new Vector3f((corner&1)==0?-radius:radius,(corner&2)==0?0:entity.getBbHeight(),(corner&4)==0?-radius:radius)
                    .rotate(rotation).add(translation);
            float px=x+point.x*scale,py=y+point.y*scale;
            left=Math.min(left,px);right=Math.max(right,px);top=Math.min(top,py);bottom=Math.max(bottom,py);
        }
        int x0=(int)Math.floor(left),y0=(int)Math.floor(top),x1=(int)Math.ceil(right),y1=(int)Math.ceil(bottom);
        GuiCapture.model(GuiCapture.command("e",null,GuiCapture.clip(),entity.getId(),preview,GuiCapture.array(x0,y0,x1,y1),scale,x,y),
                new ScreenRectangle(x0,y0,x1-x0,y1-y0));
    }
    public static void banner(Matrix4f pose,net.minecraft.client.model.geom.ModelPart flag,DyeColor color,LegacyBannerPatterns patterns) {
        float scale=(float)Math.hypot(pose.m00(),pose.m01()),x=pose.m30(),y=pose.m31();
        // Loom moves the native flag relative to its usual block model. Use
        // that part's actual origin instead of assuming the block's -32 Y.
        int x0=Math.round(x+scale*(flag.x-10)/16f),x1=Math.round(x+scale*(flag.x+10)/16f);
        int y0=Math.round(y+scale*flag.y/16f),y1=Math.round(y+scale*(flag.y+40)/16f);
        var encoded=patterns.encode();
        GuiCapture.model(GuiCapture.command("n",null,GuiCapture.clip(),color.getId(),encoded,GuiCapture.array(x0,y0,x1,y1)),new ScreenRectangle(x0,y0,x1-x0,y1-y0));
    }
}
