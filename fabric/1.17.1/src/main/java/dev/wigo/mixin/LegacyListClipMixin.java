package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wigo.client.GuiCapture;
import dev.wigo.client.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;

/** Legacy lists hide partial rows with a background that shared menus omit. */
@Mixin(AbstractSelectionList.class)
public abstract class LegacyListClipMixin {
    @WrapMethod(method="renderList")
    private void rows(PoseStack pose, int x, int y, int left, int top, float delta, Operation<Void> original) {
        if (!GuiCapture.active()) {original.call(pose,x,y,left,top,delta);return;}
        var bounds=(SelectionListAccessor)this;
        GuiCapture.scissor(GuiGraphics.wrap(pose),bounds.wigoX(),bounds.wigoY(),bounds.wigoX()+bounds.wigoWidth(),bounds.wigoBottom());
        try {original.call(pose,x,y,left,top,delta);}
        finally {GuiCapture.unscissor();}
    }
}
