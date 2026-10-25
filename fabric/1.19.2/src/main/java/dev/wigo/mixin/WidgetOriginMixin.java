package dev.wigo.mixin;

import dev.wigo.client.VanillaGui;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Screen.class)
public abstract class WidgetOriginMixin {
    @WrapMethod(method="addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;")
    private GuiEventListener wigoWidgetOrigin(GuiEventListener widget,Operation<GuiEventListener> original) {
        GuiEventListener result=original.call(widget);
        VanillaGui.widgetAdded((Screen)(Object)this,result);
        return result;
    }
}
