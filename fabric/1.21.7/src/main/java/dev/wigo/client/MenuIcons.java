package dev.wigo.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.Map;

/** Local item models, including the viewer's resource pack; no image assets are sent. */
public final class MenuIcons {
    private static final Map<String, ItemStack> icons = new HashMap<>();
    private MenuIcons() {}
    public static ItemStack item(String menuId) {
        return icons.computeIfAbsent(menuId, id -> {
            String name = id.substring(id.indexOf(':') + 1);
            String item = switch (name) {
                case "generic_9x1", "generic_9x2", "generic_9x3", "generic_9x4", "generic_9x5", "generic_9x6" -> "chest";
                case "generic_3x3" -> "dispenser";
                case "crafting" -> "crafting_table";
                case "crafter_3x3" -> "crafter";
                case "enchantment" -> "enchanting_table";
                case "smithing", "legacy_smithing" -> "smithing_table";
                case "merchant" -> "emerald";
                case "inventory" -> "leather_chestplate";
                case "creative_inventory", "game_mode" -> "grass_block";
                case "horse_inventory" -> "saddle";
                case "chat", "book_edit" -> "writable_book";
                case "book_view" -> "written_book";
                case "book_sign" -> "feather";
                case "pause" -> "clock";
                case "advancements" -> "golden_apple";
                case "statistics", "font_settings", "telemetry", "credits", "report_chat", "report_chat_selection" -> "paper";
                case "sign_edit" -> "oak_sign";
                case "hanging_sign_edit" -> "oak_hanging_sign";
                case "command_minecart" -> "command_block_minecart";
                case "jigsaw_block" -> "jigsaw";
                case "options", "settings", "video_settings", "debug_options" -> "comparator";
                case "sound_settings" -> "note_block";
                case "controls", "key_binds", "mouse_settings" -> "lever";
                case "skin_settings", "report_skin" -> "leather_chestplate";
                case "language" -> "book";
                case "chat_settings" -> "writable_book";
                case "accessibility" -> "spyglass";
                case "online_settings", "world_options" -> "compass";
                case "resource_packs" -> "painting";
                case "game_rules" -> "command_block";
                case "social_interactions", "friends" -> "player_head";
                case "report_player", "report_name", "report_reason", "unsupported" -> "barrier";
                case "other_vanilla" -> "book";
                default -> name;
            };
            var key = ResourceLocation.withDefaultNamespace(item);
            return new ItemStack(BuiltInRegistries.ITEM.containsKey(key) ? BuiltInRegistries.ITEM.getValue(key) : Items.BOOK);
        });
    }
}
