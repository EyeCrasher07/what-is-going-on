package dev.wigo.mixin;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(GuiTextRenderState.class)
public interface GuiTextAccessor {
    @Accessor("text") FormattedCharSequence wigoText();
    @Accessor("x") int wigoX();
    @Accessor("y") int wigoY();
    @Accessor("color") int wigoColor();
    @Accessor("backgroundColor") int wigoBackground();
    @Accessor("dropShadow") boolean wigoShadow();
}
