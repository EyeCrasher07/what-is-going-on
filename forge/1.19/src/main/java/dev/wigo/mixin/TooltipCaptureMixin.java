package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wigo.client.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.List;

@Mixin(Screen.class)
public abstract class TooltipCaptureMixin {
    @Shadow protected Font font;
    @WrapMethod(method="renderTooltipInternal")
    private void fit(PoseStack pose,List<ClientTooltipComponent> components,int x,int y,Operation<Void> original) {
        if(GuiCapture.tooltipCanvas()==null||components.isEmpty()) {original.call(pose,components,x,y);return;}
        int width=0,height=components.size()==1?-2:0;
        for(var component:components) {width=Math.max(width,component.getWidth(font));height+=component.getHeight();}
        var screen=(Screen)(Object)this;
        int desiredX=x+12;if(desiredX+width>screen.width)desiredX-=28+width;
        int desiredY=y-12;if(desiredY+height+6>screen.height)desiredY=screen.height-height-6;if(y-height-8<0)desiredY=y+8;
        int originX=12+width>screen.width?-16-width:12;
        int originY=8;
        LegacyTooltipLayout.draw(GuiGraphics.wrap(pose),width,height,desiredX,desiredY,originX,originY,
                ()->original.call(pose,components,0,0));
    }
}
