package dev.wigo.mixin;

import dev.wigo.client.VanillaGui;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Screen.class)
public abstract class WidgetOriginMixin {
    @WrapMethod(method="addButton")
    private Widget wigoWidgetOrigin(Widget widget,Operation<Widget> original) {
        Widget result=original.call(widget);
        VanillaGui.widgetAdded((Screen)(Object)this,result);
        return result;
    }
}
