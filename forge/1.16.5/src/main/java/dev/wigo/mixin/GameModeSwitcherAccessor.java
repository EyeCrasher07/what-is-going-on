package dev.wigo.mixin;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.screen.GamemodeSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
@Mixin(GamemodeSelectionScreen.class)
public interface GameModeSwitcherAccessor {
    @Accessor("slots") List<Widget> wigoSlots();
    @Accessor("firstMouseX") int wigoFirstX();
    @Accessor("firstMouseX") void wigoFirstX(int value);
    @Accessor("firstMouseY") int wigoFirstY();
    @Accessor("firstMouseY") void wigoFirstY(int value);
    @Accessor("setFirstMousePos") boolean wigoFirstSet();
    @Accessor("setFirstMousePos") void wigoFirstSet(boolean value);
}
