package dev.wigo.protocol;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import java.util.Set;
public final class LegacyQuickCraft {
    public static int placeCount(Set<Slot> selected,int type,ItemStack carried) {
        var preview=carried.copy();Container.getQuickCraftSlotCount(selected,type,preview,0);return preview.getCount();
    }
}
