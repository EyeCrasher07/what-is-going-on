package dev.wigo.mixin;

import dev.wigo.client.BookLayoutCache;
import net.minecraft.client.gui.screen.EditBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EditBookScreen.class)
public interface BookCacheAccessor extends BookLayoutCache {
    @Override @Invoker("clearDisplayCache") void wigoClearBookLayout();
}
