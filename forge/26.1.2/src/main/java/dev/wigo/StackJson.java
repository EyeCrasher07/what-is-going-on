package dev.wigo;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.wigo.protocol.MirrorState;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

public final class StackJson {
    private StackJson() {}
    public static JsonElement encode(ItemStack original, RegistryAccess registries) {
        if (original.isEmpty()) return JsonNull.INSTANCE;
        ItemStack stack = original.copy();
        // Contents of books and nested inventories aren't part of an item's icon.
        stack.remove(DataComponents.CONTAINER);
        stack.remove(DataComponents.BUNDLE_CONTENTS);
        stack.remove(DataComponents.WRITTEN_BOOK_CONTENT);
        stack.remove(DataComponents.WRITABLE_BOOK_CONTENT);
        stack.remove(DataComponents.LORE);
        try {
            JsonElement encoded = ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), stack).getOrThrow();
            if (encoded.toString().length() <= MirrorState.MAX_STACK_CHARS) return encoded;
        } catch (RuntimeException ignored) { }
        JsonObject small = new JsonObject();
        small.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        small.addProperty("count", stack.getCount());
        JsonObject components = new JsonObject();
        if (stack.isDamageableItem()) components.addProperty("minecraft:damage", stack.getDamageValue());
        if (stack.hasFoil()) components.addProperty("minecraft:enchantment_glint_override", true);
        if (!components.isEmpty()) small.add("components", components);
        return small;
    }

    public static ItemStack decode(JsonElement json, RegistryAccess registries) {
        if (json == null || json.isJsonNull()) return ItemStack.EMPTY;
        return ItemStack.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), json).result().orElse(ItemStack.EMPTY);
    }
}
