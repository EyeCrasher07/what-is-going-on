package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wigo.client.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.List;

@Mixin(Screen.class)
public abstract class TooltipCaptureMixin {
    @Shadow protected Font font;
    @WrapMethod(method="renderTooltipInternal")
    private void fit(PoseStack pose,List<ClientTooltipComponent> components,int x,int y,ClientTooltipPositioner positioner,Operation<Void> original) {
        if(GuiCapture.tooltipCanvas()==null||components.isEmpty()) {original.call(pose,components,x,y,positioner);return;}
        int width=0,height=components.size()==1?-2:0;
        for(var component:components) {width=Math.max(width,component.getWidth(font));height+=component.getHeight();}
        var screen=(Screen)(Object)this;
        var desired=positioner.positionTooltip(screen,x,y,width,height);
        LegacyTooltipLayout.draw(GuiGraphics.wrap(pose),width,height,desired.x(),desired.y(),0,0,
                ()->original.call(pose,components,x,y,(ClientTooltipPositioner)(s,mx,my,w,h)->new org.joml.Vector2i(0,0)));
    }
}
