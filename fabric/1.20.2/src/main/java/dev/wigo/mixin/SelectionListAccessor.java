package dev.wigo.mixin;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(AbstractSelectionList.class)
public interface SelectionListAccessor {
    @Accessor("hovered") AbstractSelectionList.Entry<?> wigoHoveredEntry();
    @Accessor("hovered") void wigoHoveredEntry(AbstractSelectionList.Entry<?> entry);
    @Accessor("x0") int wigoX();
    @Accessor("y0") int wigoY();
    @Accessor("y1") int wigoBottom();
    @Accessor("width") int wigoWidth();
    @Invoker("getRowTop") int wigoRowTop(int index);
}
