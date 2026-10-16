package dev.wigo.client;

import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import dev.wigo.client.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import dev.wigo.client.LegacyBannerPatterns;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Native model drawing into the scoped local panel target. Rectangles are absolute. */
public final class LegacyModels {
    private static void begin(GuiGraphics g) {
        g.flush();g.pose().pushPose();g.pose().last().pose().setIdentity();g.pose().last().normal().setIdentity();
    }
    private static void end(GuiGraphics g) {g.flush();g.pose().popPose();Lighting.setupFor3DItems();}
    public static void followMouse(GuiGraphics g,int x0,int y0,int x1,int y1,int scale,float offset,float mouseX,float mouseY,LivingEntity actor) {
        begin(g);
        try {InventoryScreen.renderEntityInInventory(Math.round((x0+x1)/2f),y1-3,scale,(x0+x1)/2f-mouseX,y1-53-mouseY,actor);} finally {end(g);}
    }
    public static void entity(GuiGraphics g,LivingEntity actor,float scale,Vector3f translation,Quaternionf rotation,int[] r) {
        begin(g);
        try {InventoryScreen.renderEntityInInventory((r[0]+r[2])/2,r[3]-3,Math.round(scale),0,0,actor);} finally {end(g);}
    }
    public static void book(GuiGraphics g,BookModel book,ResourceLocation texture,float scale,float open,float flip,int x0,int y0,int x1,int y1) {
        begin(g);
        try {
            Lighting.setupFor3DItems();g.pose().translate((x0+x1)/2f,(y0+y1)/2f,100);
            g.pose().scale(-scale,scale,scale);g.pose().mulPose(com.mojang.math.Vector3f.XP.rotationDegrees(25));
            g.pose().translate((1-open)*.2f,(1-open)*.1f,(1-open)*.25f);
            g.pose().mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(-(1-open)*90-90));g.pose().mulPose(com.mojang.math.Vector3f.XP.rotationDegrees(180));
            book.setupAnim(0,Mth.clamp(Mth.frac(flip+.25f)*1.6f-.3f,0,1),Mth.clamp(Mth.frac(flip+.75f)*1.6f-.3f,0,1),open);
            LegacySprites.draw(g,buffers->book.renderToBuffer(g.pose(),buffers.getBuffer(book.renderType(texture)),15728880,OverlayTexture.NO_OVERLAY,1,1,1,1));
        } finally {end(g);}
    }
    public static void banner(GuiGraphics g,ModelPart flag,DyeColor color,LegacyBannerPatterns patterns,int x0,int y0,int x1,int y1) {
        begin(g);
        try {
            Lighting.setupForFlatItems();float scale=(x1-x0)/1.25f;
            flag.y=-32;flag.xRot=0;
            g.pose().translate((x0+x1)/2f,y1-scale*.5f,0);g.pose().scale(scale,scale,-scale);
            LegacySprites.draw(g,buffers->BannerRenderer.renderPatterns(g.pose(),buffers,15728880,OverlayTexture.NO_OVERLAY,flag,ModelBakery.BANNER_BASE,true,patterns.layers));
        } finally {end(g);}
    }
    public static void skin(GuiGraphics g,LivingEntity actor,float scale,float rotationX,float rotationY,float pivot,int x0,int y0,int x1,int y1) {
        followMouse(g,x0,y0,x1,y1,Math.max(1,Math.round(scale)),.0625f,(x0+x1)/2f-rotationY,(y0+y1)/2f+rotationX,actor);
    }
}
