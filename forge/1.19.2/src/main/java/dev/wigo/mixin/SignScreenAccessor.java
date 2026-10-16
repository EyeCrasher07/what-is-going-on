package dev.wigo.mixin;

import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SignEditScreen.class)
public interface SignScreenAccessor {
    @Accessor("sign") SignBlockEntity wigoSign();
}
