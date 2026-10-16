package dev.wigo.mixin;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractWidget.class)
public interface WidgetAccessor {
    @Accessor("isHovered") boolean wigoHovered();
    @Accessor("isHovered") void wigoHovered(boolean hovered);
}
