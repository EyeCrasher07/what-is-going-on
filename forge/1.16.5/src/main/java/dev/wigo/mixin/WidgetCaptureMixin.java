package dev.wigo.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wigo.client.GuiCapture;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.widget.Widget;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Widget.class)
public abstract class WidgetCaptureMixin {
    @WrapMethod(method="render")
    private void vanillaOnly(MatrixStack g,int x,int y,float delta,Operation<Void> original) {
        boolean page=GuiCapture.decoration(this);
        if(page&&GuiCapture.capturing())return;
        if (page) GuiCapture.muted++;
        var previous=GuiCapture.widget((Widget)(Object)this);
        try {original.call(g,x,y,delta);} finally {GuiCapture.widget(previous);if(page) GuiCapture.muted--;}
    }
}
