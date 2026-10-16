package dev.wigo.mixin;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 1.16 lists compute hover during rendering and do not retain a hovered field. */
@Mixin(AbstractSelectionList.class)
public interface SelectionListAccessor {
    @Accessor("x0") int wigoX();
    @Accessor("y0") int wigoY();
    @Accessor("y1") int wigoBottom();
    @Accessor("width") int wigoWidth();
    @Invoker("getRowTop") int wigoRowTop(int index);
}
