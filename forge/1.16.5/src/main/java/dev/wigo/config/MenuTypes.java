package dev.wigo.config;

import net.minecraft.util.registry.Registry;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.EnderChestInventory;

/** Container identities come from the game registry, including custom container titles. */
public final class MenuTypes {
    private MenuTypes() {}
    public static String id(Container menu) {
        if(menu instanceof PlayerContainer)return "minecraft:inventory";
        if(menu instanceof ChestContainer chest && chest.getContainer() instanceof EnderChestInventory)return MenuCatalog.ENDER_CHEST;
        try {
            var key=Registry.MENU.getKey(menu.getType());
            return key!=null&&MenuCatalog.known(key.toString())?key.toString():MenuCatalog.OTHER;
        } catch(UnsupportedOperationException noType) {
            // Vanilla mount menus deliberately have no registered MenuType.
            return "minecraft:horse_inventory";
        }
    }
}
