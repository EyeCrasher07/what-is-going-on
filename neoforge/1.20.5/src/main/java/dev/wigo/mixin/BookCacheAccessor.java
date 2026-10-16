package dev.wigo.mixin;

import dev.wigo.client.BookLayoutCache;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BookEditScreen.class)
public interface BookCacheAccessor extends BookLayoutCache {
    @Override @Invoker("clearDisplayCache") void wigoClearBookLayout();
}
