package dev.wigo.mixin;

import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.inventory.container.Slot;
import java.util.Set;

@Mixin(ContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos") int wigoLeft();
    @Accessor("topPos") int wigoTop();
    @Accessor("imageWidth") int wigoWidth();
    @Accessor("imageHeight") int wigoHeight();
    @Accessor("isQuickCrafting") boolean wigoQuickCrafting();
    @Accessor("quickCraftingType") int wigoQuickCraftType();
    @Accessor("quickCraftSlots") Set<Slot> wigoQuickCraftSlots();
}
