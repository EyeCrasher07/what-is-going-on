package dev.wigo.mixin;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Typed accessors are remapped with Minecraft; reflected field-name strings are not. */
@Mixin(AbstractSelectionList.class)
public interface SelectionListAccessor {
    @Accessor("hovered") AbstractSelectionList.Entry<?> wigoHoveredEntry();
    @Accessor("hovered") void wigoHoveredEntry(AbstractSelectionList.Entry<?> entry);
    @Invoker("getRowTop") int wigoRowTop(int index);
}
