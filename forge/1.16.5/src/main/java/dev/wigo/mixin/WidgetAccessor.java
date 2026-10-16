package dev.wigo.mixin;

import net.minecraft.client.gui.widget.Widget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Widget.class)
public interface WidgetAccessor {
    @Accessor("isHovered") boolean wigoHovered();
    @Accessor("isHovered") void wigoHovered(boolean value);
    @Accessor("height") void wigoHeight(int value);
}
