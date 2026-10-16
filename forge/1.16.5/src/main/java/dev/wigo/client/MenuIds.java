package dev.wigo.client;

import dev.wigo.config.MenuCatalog;
import dev.wigo.config.MenuTypes;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.GamemodeSelectionScreen;
import net.minecraft.client.gui.screen.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.EditBookScreen;
import net.minecraft.client.gui.screen.EditMinecartCommandBlockScreen;
import net.minecraft.client.gui.screen.EditSignScreen;
import net.minecraft.client.gui.screen.EditStructureScreen;
import net.minecraft.client.gui.screen.JigsawScreen;
import net.minecraft.client.gui.screen.ReadBookScreen;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.util.text.TranslationTextComponent;

public final class MenuIds {
    private MenuIds() {}
    public static String of(Screen screen,String kind) {
        if(screen==null||kind.equals("none"))return "";
        if(WigoLinks.owns(screen))return MenuCatalog.SETTINGS;
        if(screen instanceof CreativeScreen)return "minecraft:creative_inventory";
        if(screen instanceof ChestScreen && screen.getTitle() instanceof TranslationTextComponent title
                && title.getKey().equals("container.enderchest"))return MenuCatalog.ENDER_CHEST;
        if(screen instanceof ContainerScreen<?> container)return MenuTypes.id(container.getMenu());
        if(screen instanceof ChatScreen)return "minecraft:chat";
        if(screen instanceof IngameMenuScreen)return "minecraft:pause";
        if(screen instanceof GamemodeSelectionScreen)return "minecraft:game_mode";
        if(screen instanceof EditMinecartCommandBlockScreen)return "minecraft:command_minecart";
        if(screen instanceof AbstractCommandBlockScreen)return "minecraft:command_block";
        if(screen instanceof EditStructureScreen)return "minecraft:structure_block";
        if(screen instanceof JigsawScreen)return "minecraft:jigsaw_block";
        if(screen.getTitle() instanceof TranslationTextComponent title) {
            String id=MenuCatalog.forTitle(title.getKey());if(!id.equals(MenuCatalog.OTHER))return id;
        }
        if(screen instanceof EditBookScreen)return "minecraft:book_edit";
        if(screen instanceof ReadBookScreen)return "minecraft:book_view";
        if(screen instanceof EditSignScreen)return "minecraft:sign_edit";
        return kind.equals("unsupported")?MenuCatalog.UNSUPPORTED:MenuCatalog.OTHER;
    }
}
