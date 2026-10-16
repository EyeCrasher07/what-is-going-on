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
import net.minecraft.client.model.BannerFlagModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.joml.Matrix3x2f;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import java.util.*;

/** All artwork, fonts, item models and atlases are resolved on the observer's client. */
public final class GuiReplay implements AutoCloseable {
    private final Map<Integer, Cached> cache = new HashMap<>();
    private final UUID cacheId=UUID.randomUUID();
    private BookModel book;
    private BannerFlagModel banner;
    private net.minecraft.client.model.PlayerModel skin;

    public void draw(GuiGraphics g, RemoteMenu menu, Player actor, int pad) {
        GuiScene scene = menu.scene;
        if (scene == null || !scene.ready) return;
        Minecraft mc = Minecraft.getInstance();
        g.pose().pushMatrix();
        g.pose().scale(scene.scale(), scene.scale());
        g.pose().translate(-scene.left, -scene.top);
        g.enableScissor(Math.round(scene.left),Math.round(scene.top),Math.round(scene.left+scene.rawWidth),Math.round(scene.top+scene.rawHeight));
        JsonArray[] commands = scene.commands();
        var stale=cache.entrySet().iterator();
        while (stale.hasNext()) {var entry=stale.next();if (entry.getKey()>=commands.length) {release(entry.getValue());stale.remove();}}
        try {
        for (int index = 0; index < commands.length; index++) {
            JsonArray c = commands[index];
            boolean clipped = !c.get(2).isJsonNull();
            if (clipped) { JsonArray r = c.get(2).getAsJsonArray(); g.enableScissor(integer(r,0),integer(r,1),integer(r,2),integer(r,3)); }
            g.pose().pushMatrix();
            if (!c.get(1).isJsonNull()) {
                JsonArray p = c.get(1).getAsJsonArray();
                g.pose().mul(new Matrix3x2f(number(p,0),number(p,1),number(p,2),number(p,3),number(p,4),number(p,5)));
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
                            g.pose().pushMatrix();g.pose().translate(anchor-(float)Math.floor(width*factor/2),y);g.pose().scale(factor,factor);
                            g.drawString(mc.font,text,0,0,integer(c,6),c.get(7).getAsBoolean());g.pose().popMatrix();
                        }else g.drawString(mc.font,text,x,y,integer(c,6),c.get(7).getAsBoolean());
                    }
                    case "b" -> blit(g,ResourceLocation.parse(c.get(3).getAsString()),c,4,number(c,8),number(c,9),number(c,10),number(c,11),integer(c,12));
                    case "a" -> {
                        // Commands carry the texture location, not AtlasManager's logical atlas ID.
                        TextureAtlasSprite sprite=((TextureAtlas)mc.getTextureManager().getTexture(ResourceLocation.parse(c.get(3).getAsString()))).getSprite(ResourceLocation.parse(c.get(4).getAsString()));
                        blit(g,sprite.atlasLocation(),c,5,lerp(sprite.getU0(),sprite.getU1(),number(c,9)),lerp(sprite.getU0(),sprite.getU1(),number(c,10)),
                                lerp(sprite.getV0(),sprite.getV1(),number(c,11)),lerp(sprite.getV0(),sprite.getV1(),number(c,12)),integer(c,13));
                    }
                    case "s" -> {
                        ResourceLocation id=ResourceLocation.parse(c.get(3).getAsString());
                        if (integer(c,4)==0) g.blitSprite(RenderPipelines.GUI_TEXTURED,id,integer(c,8),integer(c,9),integer(c,10),integer(c,11),integer(c,12));
                        else g.blitSprite(RenderPipelines.GUI_TEXTURED,id,integer(c,4),integer(c,5),integer(c,6),integer(c,7),integer(c,8),integer(c,9),integer(c,10),integer(c,11),integer(c,12));
                    }
                    case "i" -> {
                        g.nextStratum();g.renderItem(actor,item(index,c),integer(c,4),integer(c,5),integer(c,6));g.nextStratum();
                    }
                    case "e" -> {
                        LivingEntity entity=entity(index,c,actor);
                        if (entity!=null) { int[] r=rect(c.get(5).getAsJsonArray(),scene,pad);
                            InventoryScreen.renderEntityInInventoryFollowsMouse(g,r[0],r[1],r[2],r[3],Math.max(1,Math.round(number(c,6)*scene.scale())),0.0625f,
                                    pad+(number(c,7)-scene.left)*scene.scale(),pad+(number(c,8)-scene.top)*scene.scale(),entity); }
                    }
                    case "k" -> {
                        if (book==null) book=new BookModel(mc.getEntityModels().bakeLayer(ModelLayers.BOOK));
                        int[] r=rect(c.get(4).getAsJsonArray(),scene,pad);
                        g.submitBookModelRenderState(book,ResourceLocation.parse(c.get(3).getAsString()),number(c,5)*scene.scale(),number(c,6),number(c,7),r[0],r[1],r[2],r[3]);
                    }
                    case "n" -> {
                        if (banner==null) banner=new BannerFlagModel(mc.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
                        Cached cached=cached(index,c); if (cached.value==null) cached.value=BannerPatternLayers.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE,mc.level.registryAccess()),c.get(4)).getOrThrow();
                        int[] r=rect(c.get(5).getAsJsonArray(),scene,pad);
                        g.submitBannerPatternRenderState(banner.root().getChild("flag"),DyeColor.byId(integer(c,3)),(BannerPatternLayers)cached.value,r[0],r[1],r[2],r[3]);
                    }
                    case "v" -> {
                        if (skin==null) skin=new net.minecraft.client.model.PlayerModel(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER),false);
                        int[] r=rect(c.get(3).getAsJsonArray(),scene,pad);
                        AbstractClientPlayer player=actor instanceof AbstractClientPlayer p?p:mc.player;
                        g.submitSkinRenderState(skin,player.getSkin().texture(),number(c,4)*scene.scale(),number(c,5),number(c,6),number(c,7),r[0],r[1],r[2],r[3]);
                    }
                    case "o" -> {
                        Cached cached=cached(index,c);
                        String name=c.get(3).getAsString().substring("minecraft:".length());
                        var wood=net.minecraft.world.level.block.state.properties.WoodType.values().filter(w->w.name().equals(name)).findFirst().orElseThrow();
                        if(cached.value==null)cached.value=net.minecraft.client.renderer.blockentity.SignRenderer.createSignModel(mc.getEntityModels(),wood,c.get(6).getAsBoolean());
                        int[] r=rect(c.get(4).getAsJsonArray(),scene,pad);
                        g.nextStratum();g.submitSignRenderState((net.minecraft.client.model.Model)cached.value,number(c,5)*scene.scale(),wood,r[0],r[1],r[2],r[3]);g.nextStratum();
                    }
                    case "l" -> throw new IllegalArgumentException("Legacy sign model is unavailable on this version");
                    case "z" -> g.nextStratum();
                    case "q" -> { /* Full chat draft is shown only above the observer's own input. */ }
                    case "h" -> {
                        Cached cached=cached(index,c);
                        if (cached.value==null) {
                            ArmorStandRenderState preview=new ArmorStandRenderState();preview.entityType=EntityType.ARMOR_STAND;
                            preview.showBasePlate=false;preview.showArms=true;preview.xRot=25;preview.bodyRot=210;
                            ItemStack stack=StackJson.decode(c.get(3),mc.level.registryAccess());
                            var equippable=stack.get(DataComponents.EQUIPPABLE);EquipmentSlot slot=equippable==null?EquipmentSlot.OFFHAND:equippable.slot();
                            switch (slot) {case HEAD->preview.headEquipment=stack;case CHEST->preview.chestEquipment=stack;case LEGS->preview.legsEquipment=stack;case FEET->preview.feetEquipment=stack;
                                default->{mc.getItemModelResolver().updateForTopItem(preview.leftHandItem,stack,ItemDisplayContext.THIRD_PERSON_LEFT_HAND,mc.level,null,0);}}
                            cached.value=preview;
                        }
                        int[] r=rect(c.get(4).getAsJsonArray(),scene,pad);JsonArray t=c.get(6).getAsJsonArray(),q=c.get(7).getAsJsonArray();
                        g.submitEntityRenderState((ArmorStandRenderState)cached.value,number(c,5)*scene.scale(),new Vector3f(number(t,0),number(t,1),number(t,2)),new Quaternionf(number(q,0),number(q,1),number(q,2),number(q,3)),null,r[0],r[1],r[2],r[3]);
                    }
                    case "m" -> {
                        Cached cached=cached(index,c);
                        if (cached.value==null) {
                            byte[] colors=MapPalette.decode(c.get(4).getAsString());
                            DynamicTexture texture=new DynamicTexture("WIGO temporary map palette",128,32,false);
                            for (int i=0;i<colors.length;i++) texture.getPixels().setPixel(i%128,i/128,MapColor.getColorFromPackedId(Byte.toUnsignedInt(colors[i])));
                            texture.upload();ResourceLocation id=ResourceLocation.fromNamespaceAndPath("wigo","map/"+cacheId+"/"+index);
                            mc.getTextureManager().register(id,texture);cached.value=id;
                        }
                        int y=integer(c,3);
                        g.blit((ResourceLocation)cached.value,0,y,128,y+32,0,1,0,1);
                    }
                    default -> throw new IllegalArgumentException("Unsupported vanilla drawing");
                }
            } finally { g.pose().popMatrix(); if (clipped) g.disableScissor(); }
        }
        } finally {g.disableScissor();g.pose().popMatrix();}
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
            var type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(ResourceLocation.parse(preview.get("type").getAsString()));
            Entity entity=type==null?null:type.create(mc.level,EntitySpawnReason.LOAD);
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
        ((GraphicsInvoker)g).wigoBlit(RenderPipelines.GUI_TEXTURED,texture,integer(c,start),integer(c,start+2),integer(c,start+1),integer(c,start+3),u0,u1,v0,v1,color);
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
