package dev.wigo.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(targets="net.minecraft.client.gui.screen.GamemodeSelectionScreen$SelectorWidget")
public interface GameModeSlotAccessor {
    @Accessor("isSelected") boolean wigoSelected();
    @Accessor("isSelected") void wigoSelected(boolean selected);
}
