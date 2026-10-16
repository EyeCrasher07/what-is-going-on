package dev.wigo.config;

import net.minecraft.core.Registry;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;

/** Container identities come from the game registry, including custom container titles. */
public final class MenuTypes {
    private MenuTypes() {}
    public static String id(AbstractContainerMenu menu) {
        if(menu instanceof InventoryMenu)return "minecraft:inventory";
        if(menu instanceof ChestMenu chest && chest.getContainer() instanceof PlayerEnderChestContainer)return MenuCatalog.ENDER_CHEST;
        try {
            var key=Registry.MENU.getKey(menu.getType());
            return key!=null&&MenuCatalog.known(key.toString())?key.toString():MenuCatalog.OTHER;
        } catch(UnsupportedOperationException noType) {
            // Vanilla mount menus deliberately have no registered MenuType.
            return "minecraft:horse_inventory";
        }
    }
}
