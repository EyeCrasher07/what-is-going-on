package dev.wigo.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.world.inventory.Slot;
import java.util.Set;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos") int wigoLeft();
    @Accessor("topPos") int wigoTop();
    @Accessor("imageWidth") int wigoWidth();
    @Accessor("imageHeight") int wigoHeight();
    @Accessor("isQuickCrafting") boolean wigoQuickCrafting();
    @Accessor("quickCraftingType") int wigoQuickCraftType();
    @Accessor("quickCraftSlots") Set<Slot> wigoQuickCraftSlots();
}
