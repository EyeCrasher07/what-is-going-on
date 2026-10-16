package dev.wigo.client;

import net.minecraft.world.item.ItemStack;

public final class LegacyStacks {
    public static ItemStack count(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }
}
