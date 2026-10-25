package dev.wigo.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wigo.client.GuiCapture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(AbstractWidget.class)
public abstract class WidgetCaptureMixin {
    @WrapMethod(method="extractRenderState")
    private void vanillaOnly(GuiGraphicsExtractor g,int x,int y,float delta,Operation<Void> original) {
        boolean page=GuiCapture.decoration(this);
        if(page&&GuiCapture.capturing())return;
        if (page) GuiCapture.muted++;
        var previous=GuiCapture.widget((AbstractWidget)(Object)this);
        try {original.call(g,x,y,delta);} finally {GuiCapture.widget(previous);if(page) GuiCapture.muted--;}
    }
}
