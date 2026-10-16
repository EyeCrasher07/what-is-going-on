package dev.wigo.protocol;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.Set;
public final class LegacyQuickCraft {
    public static int placeCount(Set<Slot> selected,int type,ItemStack carried) {
        var preview=carried.copy();AbstractContainerMenu.getQuickCraftSlotCount(selected,type,preview,0);return preview.getCount();
    }
}
