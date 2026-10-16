package dev.wigo.mixin;

import net.minecraft.client.gui.screen.EditSignScreen;
import net.minecraft.tileentity.SignTileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EditSignScreen.class)
public interface SignScreenAccessor {
    @Accessor("sign") SignTileEntity wigoSign();
}
