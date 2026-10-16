package dev.wigo.client;

import dev.wigo.config.MenuCatalog;
import dev.wigo.config.MenuTypes;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class MenuIds {
    private MenuIds() {}
    public static String of(Screen screen,String kind) {
        if(screen==null||kind.equals("none"))return "";
        if(WigoLinks.owns(screen))return MenuCatalog.SETTINGS;
        if(screen instanceof CreativeModeInventoryScreen)return "minecraft:creative_inventory";
        if(screen instanceof ContainerScreen && screen.getTitle().getContents() instanceof TranslatableContents title
                && title.getKey().equals("container.enderchest"))return MenuCatalog.ENDER_CHEST;
        if(screen instanceof AbstractContainerScreen<?> container)return MenuTypes.id(container.getMenu());
        if(screen instanceof ChatScreen)return "minecraft:chat";
        if(screen instanceof PauseScreen)return "minecraft:pause";
        if(screen instanceof GameModeSwitcherScreen)return "minecraft:game_mode";
        if(screen instanceof MinecartCommandBlockEditScreen)return "minecraft:command_minecart";
        if(screen instanceof AbstractCommandBlockEditScreen)return "minecraft:command_block";
        if(screen instanceof StructureBlockEditScreen)return "minecraft:structure_block";
        if(screen instanceof JigsawBlockEditScreen)return "minecraft:jigsaw_block";
        if(screen.getTitle().getContents() instanceof TranslatableContents title) {
            String id=MenuCatalog.forTitle(title.getKey());if(!id.equals(MenuCatalog.OTHER))return id;
        }
        if(screen instanceof BookEditScreen)return "minecraft:book_edit";
        if(screen instanceof BookViewScreen)return "minecraft:book_view";
        if(screen instanceof AbstractSignEditScreen)return "minecraft:sign_edit";
        return kind.equals("unsupported")?MenuCatalog.UNSUPPORTED:MenuCatalog.OTHER;
    }
}
