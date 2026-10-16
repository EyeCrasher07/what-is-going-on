package dev.wigo.protocol;

import com.google.gson.*;

/** Copy mutable objects/arrays without relying on newer Gson methods. */
public final class LegacyJson {
    private LegacyJson() {}
    public static java.util.Set<String> keys(JsonObject value) {
        var keys=new java.util.LinkedHashSet<String>();for(var entry:value.entrySet())keys.add(entry.getKey());return keys;
    }
    @SuppressWarnings("unchecked")
    public static <T extends JsonElement> T copy(T value) {
        if (value.isJsonObject()) {
            JsonObject result=new JsonObject();
            for(var entry:value.getAsJsonObject().entrySet())result.add(entry.getKey(),copy(entry.getValue()));
            return (T)result;
        }
        if (value.isJsonArray()) {
            JsonArray result=new JsonArray();
            for(JsonElement element:value.getAsJsonArray())result.add(copy(element));
            return (T)result;
        }
        return value;
    }
}
