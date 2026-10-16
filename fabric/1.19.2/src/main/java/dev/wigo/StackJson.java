package dev.wigo;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.wigo.protocol.MirrorState;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Send only NBT needed to render an icon, never nested inventory/book contents. */
public final class StackJson {
    private StackJson() {}
    public static JsonElement encode(ItemStack original,RegistryAccess registries) {
        if(original.isEmpty())return JsonNull.INSTANCE;
        ItemStack stack=original.copy();
        CompoundTag tag=stack.getTag();
        if(tag!=null) {
            for(String key:new String[]{"Items","pages","filtered_pages","EntityTag"})tag.remove(key);
            if(tag.contains("BlockEntityTag")) {
                CompoundTag block=tag.getCompound("BlockEntityTag");
                for(String key:new String[]{"Items","LootTable","LootTableSeed"})block.remove(key);
            }
            if(tag.contains("display"))tag.getCompound("display").remove("Lore");
        }
        try {
            JsonObject encoded=ItemStack.CODEC.encodeStart(JsonOps.INSTANCE,stack).result().orElseThrow().getAsJsonObject();
            encoded.add("count",encoded.remove("Count"));
            if(encoded.toString().length()<=MirrorState.MAX_STACK_CHARS)return encoded;
        } catch(RuntimeException ignored) {}
        JsonObject small=new JsonObject();
        small.addProperty("id",Registry.ITEM.getKey(stack.getItem()).toString());
        small.addProperty("count",stack.getCount());
        JsonObject minimal=new JsonObject();
        if(stack.isDamageableItem())minimal.addProperty("Damage",stack.getDamageValue());
        if(stack.hasFoil()) {JsonArray enchantments=new JsonArray();JsonObject effect=new JsonObject();effect.addProperty("id","minecraft:unbreaking");effect.addProperty("lvl",1);enchantments.add(effect);minimal.add("Enchantments",enchantments);}
        if(minimal.size()!=0)small.add("tag",minimal);
        return small;
    }
    public static ItemStack decode(JsonElement json,RegistryAccess registries) {
        if(json==null||json.isJsonNull())return ItemStack.EMPTY;
        JsonObject nativeJson=json.getAsJsonObject().deepCopy();
        if(nativeJson.has("count"))nativeJson.add("Count",nativeJson.remove("count"));
        return ItemStack.CODEC.parse(JsonOps.INSTANCE,nativeJson).result().orElse(ItemStack.EMPTY);
    }
}
