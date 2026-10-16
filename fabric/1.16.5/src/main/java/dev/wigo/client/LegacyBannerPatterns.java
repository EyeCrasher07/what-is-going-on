package dev.wigo.client;

import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import java.util.*;

/** Preserve local enum pattern names across the observer's vanilla banner renderer. */
public final class LegacyBannerPatterns {
    public final List<Pair<BannerPattern,DyeColor>> layers;
    public LegacyBannerPatterns(List<Pair<BannerPattern,DyeColor>> layers) {this.layers=List.copyOf(layers);}
    public JsonArray encode() {
        JsonArray result=new JsonArray();
        for(var layer:layers.subList(0,Math.min(17,layers.size()))) {
            JsonObject entry=new JsonObject();
            entry.addProperty("pattern","minecraft:"+layer.getFirst().getFilename());
            entry.addProperty("color",layer.getSecond().getId());result.add(entry);
        }
        return result;
    }
    public static LegacyBannerPatterns decode(JsonElement encoded) {
        List<Pair<BannerPattern,DyeColor>> result=new ArrayList<>();
        for(JsonElement element:encoded.getAsJsonArray()) {
            if(result.size()==17)break;
            JsonObject entry=element.getAsJsonObject();
            String name=entry.get("pattern").getAsString();
            BannerPattern pattern=java.util.Arrays.stream(BannerPattern.values()).filter(p->p.getFilename().equals(name.substring(name.indexOf(':')+1))).findFirst().orElse(null);
            if(pattern!=null)result.add(Pair.of(pattern,DyeColor.byId(entry.get("color").getAsInt())));
        }
        return new LegacyBannerPatterns(result);
    }
}
