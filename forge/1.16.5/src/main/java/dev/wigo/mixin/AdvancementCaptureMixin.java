package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.matrix.MatrixStack;
import dev.wigo.client.GuiCapture;
import dev.wigo.client.GuiGraphics;
import net.minecraft.client.gui.advancements.AdvancementTabGui;
import org.spongepowered.asm.mixin.Mixin;

/** Replace the old depth-buffer mask with an explicit instruction clip. */
@Mixin(AdvancementTabGui.class)
public abstract class AdvancementCaptureMixin {
    @WrapMethod(method="drawContents")
    private void content(MatrixStack pose,Operation<Void> original) {
        if (!GuiCapture.recording()) {original.call(pose);return;}
        GuiCapture.scissor(GuiGraphics.wrap(pose),0,0,234,113);
        try {original.call(pose);}
        finally {GuiCapture.unscissor();}
    }
}
