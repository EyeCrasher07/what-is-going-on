package dev.wigo.client;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.wigo.StackJson;
import dev.wigo.mixin.GraphicsInvoker;
import dev.wigo.protocol.GuiScene;
import dev.wigo.protocol.MapPalette;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import dev.wigo.client.LegacyBannerPatterns;
import org.joml.Matrix3x2f;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import java.util.*;

/** All artwork, fonts, item models and atlases are resolved on the observer's client. */
public final class GuiReplay implements AutoCloseable {
    private final Map<Integer, Cached> cache = new HashMap<>();
    private final UUID cacheId=UUID.randomUUID();
    private BookModel book;
    private ModelPart banner;
    private net.minecraft.client.model.PlayerModel skin;

    public void draw(GuiGraphics g, RemoteMenu menu, Player actor, int pad) {
        GuiScene scene = menu.scene;
        if (scene == null || !scene.ready) return;
        Minecraft mc = Minecraft.getInstance();
        g.pose().pushPose();
        g.pose().scale(scene.scale(), scene.scale(),1);
        g.pose().translate(-scene.left, -scene.top,0);
        g.enableScissor(Math.round(scene.left),Math.round(scene.top),Math.round(scene.left+scene.rawWidth),Math.round(scene.top+scene.rawHeight));
        JsonArray[] commands = scene.commands();
        var stale=cache.entrySet().iterator();
        while (stale.hasNext()) {var entry=stale.next();if (entry.getKey()>=commands.length) {release(entry.getValue());stale.remove();}}
        try {
        for (int index = 0; index < commands.length; index++) {
            JsonArray c = commands[index];
            boolean clipped = !c.get(2).isJsonNull();
            if (clipped) { JsonArray r = c.get(2).getAsJsonArray(); g.enableScissor(integer(r,0),integer(r,1),integer(r,2),integer(r,3)); }
            g.pose().pushPose();
            if (!c.get(1).isJsonNull()) {
                JsonArray p = c.get(1).getAsJsonArray();
                g.pose().mulPoseMatrix(new org.joml.Matrix4f().m00(number(p,0)).m01(number(p,1)).m10(number(p,2)).m11(number(p,3)).m30(number(p,4)).m31(number(p,5)));
            }
            try {
                switch (c.get(0).getAsString()) {
                    case "c" -> {
                        if(LegacyTextCursor.isCursorVisible(Math.max(0,WigoClient.serverTime()-c.get(8).getAsLong()))) {
                            if(c.get(6).getAsBoolean())LegacyTextCursor.extractInsertCursor(g,integer(c,3),integer(c,4),integer(c,5),10);
                            else LegacyTextCursor.extractAppendCursor(g,mc.font,integer(c,3),integer(c,4),integer(c,5),c.get(7).getAsBoolean());
                        }
                    }
                    case "r" -> g.fillGradient(integer(c,3),integer(c,4),integer(c,5),integer(c,6),integer(c,7),integer(c,8));
                    case "t" -> {
                        int x=integer(c,4),y=integer(c,5),bg=integer(c,8); var text=GuiTextStyles.text(c);
                        if ((bg >>> 24) != 0) g.fill(x-1,y-1,x+mc.font.width(text)+1,y+9,bg);
                        if(c.size()==11){
                            var placement=c.get(10).getAsJsonArray();float anchor=placement.get(1).getAsFloat();int width=mc.font.width(text),limit=placement.get(2).getAsInt();
                            float factor=limit>1&&width>limit?limit/(float)width:1;
                            g.pose().pushPose();g.pose().translate(anchor-(float)Math.floor(width*factor/2),y,0);g.pose().scale(factor,factor,1);
                            g.drawString(mc.font,text,0,0,integer(c,6),c.get(7).getAsBoolean());g.pose().popPose();
                        }else g.drawString(mc.font,text,x,y,integer(c,6),c.get(7).getAsBoolean());
                    }
                    case "b" -> blit(g,new ResourceLocation(c.get(3).getAsString()),c,4,number(c,8),number(c,9),number(c,10),number(c,11),integer(c,12));
                    case "a" -> {
                        Cached cached=cached(index,c);
                        if(cached.value==null)cached.value=((TextureAtlas)mc.getTextureManager().getTexture(new ResourceLocation(c.get(3).getAsString()))).getSprite(new ResourceLocation(c.get(4).getAsString()));
                        float u0=number(c,9),u1=number(c,10),v0=number(c,11),v1=number(c,12);
                        LegacySprites.atlas(g,(TextureAtlasSprite)cached.value,integer(c,5),integer(c,6),integer(c,7),integer(c,8),u0,u1,v0,v1,integer(c,13));
                    }
                    case "s" -> throw new IllegalArgumentException("GUI sprite atlas unavailable on this version");
                    case "i" -> {
                        LegacyReplayLayers.advance(g);g.renderItem(actor,item(index,c),integer(c,4),integer(c,5),integer(c,6));LegacyReplayLayers.advance(g);
                    }
                    case "e" -> {
                        LivingEntity entity=entity(index,c,actor);
                        if(entity!=null) {
                            float x=pad+(number(c,7)-scene.left)*scene.scale(),y=pad+(number(c,8)-scene.top)*scene.scale();
                            if(c.get(4).getAsJsonObject().has("transform"))
                                LegacyModels.preview(g,entity,x,y,number(c,6)*scene.scale(),c.get(4).getAsJsonObject().getAsJsonArray("transform"));
                            else {
                                int[] r=rect(c.get(5).getAsJsonArray(),scene,pad);
                                LegacyModels.followMouse(g,r[0],r[1],r[2],r[3],Math.max(1,Math.round(number(c,6)*scene.scale())),.0625f,x,y,entity);
                            }
                        }
                    }
                    case "k" -> {
                        if (book==null) book=new BookModel(mc.getEntityModels().bakeLayer(ModelLayers.BOOK));
                        int[] r=rect(c.get(4).getAsJsonArray(),scene,pad);
                        LegacyModels.book(g,book,new ResourceLocation(c.get(3).getAsString()),number(c,5)*scene.scale(),number(c,6),number(c,7),r[0],r[1],r[2],r[3]);
                    }
                    case "n" -> {
                        if (banner==null) banner=mc.getEntityModels().bakeLayer(ModelLayers.BANNER).getChild("flag");
                        Cached cached=cached(index,c); if (cached.value==null) cached.value=LegacyBannerPatterns.decode(c.get(4));
                        int[] r=rect(c.get(5).getAsJsonArray(),scene,pad);
                        LegacyModels.banner(g,banner,DyeColor.byId(integer(c,3)),(LegacyBannerPatterns)cached.value,r[0],r[1],r[2],r[3]);
                    }
                    case "v" -> {
                        if (skin==null) skin=new net.minecraft.client.model.PlayerModel(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER),false);
                        int[] r=rect(c.get(3).getAsJsonArray(),scene,pad);
                        AbstractClientPlayer player=actor instanceof AbstractClientPlayer p?p:mc.player;
                        LegacyModels.skin(g,player,number(c,4)*scene.scale(),number(c,5),number(c,6),number(c,7),r[0],r[1],r[2],r[3]);
                    }
                    case "o" -> throw new IllegalArgumentException("Sign PIP model is unavailable on this version");
                    case "l" -> {
                        Cached cached=cached(index,c);var texture=new ResourceLocation(c.get(3).getAsString());
                        if(cached.value==null)cached.value=LegacySignModels.create(texture);
                        LegacyReplayLayers.advance(g);LegacySignModels.draw(g,cached.value,texture,c.get(4).getAsBoolean());LegacyReplayLayers.advance(g);
                    }
                    case "z" -> LegacyReplayLayers.advance(g);
                    case "q" -> { /* Full chat draft is shown only above the observer's own input. */ }
                    case "h" -> { throw new IllegalArgumentException("Unexpected preview state"); }
                    case "m" -> {
                        Cached cached=cached(index,c);
                        if (cached.value==null) {
                            byte[] colors=MapPalette.decode(c.get(4).getAsString());
                            DynamicTexture texture=new DynamicTexture(128,32,false);
                            for (int i=0;i<colors.length;i++) texture.getPixels().setPixelRGBA(i%128,i/128,MapColor.getColorFromPackedId(Byte.toUnsignedInt(colors[i])));
                            texture.upload();ResourceLocation id=new ResourceLocation("wigo","map/"+cacheId+"/"+index);
                            mc.getTextureManager().register(id,texture);cached.value=id;
                        }
                        int y=integer(c,3);
                        LegacySprites.blit(g,(ResourceLocation)cached.value,0,128,y,y+32,0,1,0,1,-1);
                    }
                    default -> throw new IllegalArgumentException("Unsupported vanilla drawing");
                }
            } finally { g.pose().popPose(); if (clipped) g.disableScissor(); }
        }
        } finally {g.disableScissor();g.pose().popPose();}
    }
    private ItemStack item(int index,JsonArray c) {
        Cached cached=cached(index,c);
        if (cached.value==null) cached.value=StackJson.decode(c.get(3),Minecraft.getInstance().level.registryAccess());
        return (ItemStack)cached.value;
    }
    private LivingEntity entity(int index,JsonArray c,Player actor) {
        Minecraft mc=Minecraft.getInstance(); JsonObject preview=c.get(4).getAsJsonObject();
        if (!preview.has("type")) { Entity entity=mc.level.getEntity(integer(c,3)); return entity instanceof LivingEntity living?living:actor; }
        Cached cached=cached(index,c);
        if (cached.value==null) {
            var type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(preview.get("type").getAsString()));
            Entity entity=type==null?null:type.create(mc.level);
            if (!(entity instanceof LivingEntity living)) return null;
            for (var entry:preview.getAsJsonObject("equipment").entrySet()) living.setItemSlot(EquipmentSlot.byName(entry.getKey()),StackJson.decode(entry.getValue(),mc.level.registryAccess()));
            if(preview.has("angles")) {
                var angles=preview.getAsJsonArray("angles");living.yBodyRot=living.yBodyRotO=number(angles,0);
                living.setXRot(number(angles,1));living.setYRot(number(angles,2));living.yHeadRot=living.yHeadRotO=number(angles,3);
            }
            if(preview.has("stand")&&living instanceof net.minecraft.world.entity.decoration.ArmorStand stand) {
                var flags=preview.getAsJsonArray("stand");stand.setShowArms(flags.get(0).getAsBoolean());stand.setNoBasePlate(flags.get(1).getAsBoolean());
            }
            cached.value=living;
        }
        return (LivingEntity)cached.value;
    }
    private Cached cached(int index,JsonArray command) {
        Cached existing=cache.get(index);
        if (existing==null||!existing.command.equals(command)) {if (existing!=null) release(existing);existing=new Cached(command);cache.put(index,existing);} return existing;
    }
    private static void blit(GuiGraphics g,ResourceLocation texture,JsonArray c,int start,float u0,float u1,float v0,float v1,int color) {
        LegacySprites.blit(g,texture,integer(c,start),integer(c,start+2),integer(c,start+1),integer(c,start+3),u0,u1,v0,v1,color);
    }
    private static int[] rect(JsonArray r,GuiScene scene,int pad) {
        return new int[]{pad+Math.round((number(r,0)-scene.left)*scene.scale()),pad+Math.round((number(r,1)-scene.top)*scene.scale()),
                pad+Math.round((number(r,2)-scene.left)*scene.scale()),pad+Math.round((number(r,3)-scene.top)*scene.scale())};
    }
    private static float lerp(float a,float b,float f) {return a+(b-a)*f;}
    private static float number(JsonArray c,int i) {return c.get(i).getAsFloat();}
    private static int integer(JsonArray c,int i) {return c.get(i).getAsInt();}
    private static void release(Cached cached) {if (cached.value instanceof ResourceLocation id) Minecraft.getInstance().getTextureManager().release(id);}
    @Override public void close() {cache.values().forEach(GuiReplay::release);cache.clear();book=null;banner=null;skin=null;}
    private static final class Cached {final JsonArray command;Object value;Cached(JsonArray c){command=c;}}
}
