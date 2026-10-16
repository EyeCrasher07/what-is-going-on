package dev.wigo.client;

import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import java.util.*;

/** Local native banner layers described by registry names and dye IDs. */
public final class LegacyBannerPatterns {
    public final List<Pair<Holder<BannerPattern>,DyeColor>> layers;
    public LegacyBannerPatterns(List<Pair<Holder<BannerPattern>,DyeColor>> layers) {this.layers=List.copyOf(layers);}
    public JsonArray encode() {
        JsonArray result=new JsonArray();
        for(var layer:layers.subList(0,Math.min(17,layers.size()))) {
            JsonObject entry=new JsonObject();entry.addProperty("pattern",BuiltInRegistries.BANNER_PATTERN.getKey(layer.getFirst().value()).toString());entry.addProperty("color",layer.getSecond().getId());result.add(entry);
        }
        return result;
    }
    public static LegacyBannerPatterns decode(JsonElement encoded) {
        List<Pair<Holder<BannerPattern>,DyeColor>> result=new ArrayList<>();
        for(JsonElement element:encoded.getAsJsonArray()) {
            if(result.size()==17)break;
            JsonObject entry=element.getAsJsonObject();var key=new ResourceLocation(entry.get("pattern").getAsString());
            BuiltInRegistries.BANNER_PATTERN.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BANNER_PATTERN,key)).ifPresent(holder->result.add(Pair.of(holder,DyeColor.byId(entry.get("color").getAsInt()))));
        }
        return new LegacyBannerPatterns(result);
    }
}
