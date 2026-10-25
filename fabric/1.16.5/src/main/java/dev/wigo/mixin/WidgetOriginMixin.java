package dev.wigo.mixin;

import dev.wigo.client.VanillaGui;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Screen.class)
public abstract class WidgetOriginMixin {
    @WrapMethod(method="addButton")
    private AbstractWidget wigoWidgetOrigin(AbstractWidget widget,Operation<AbstractWidget> original) {
        AbstractWidget result=original.call(widget);
        VanillaGui.widgetAdded((Screen)(Object)this,result);
        return result;
    }
}
