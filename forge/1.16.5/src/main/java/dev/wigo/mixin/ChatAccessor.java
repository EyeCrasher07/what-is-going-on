package dev.wigo.mixin;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ChatScreen.class)
public interface ChatAccessor {
    @Accessor("input") TextFieldWidget wigoInput();
}
