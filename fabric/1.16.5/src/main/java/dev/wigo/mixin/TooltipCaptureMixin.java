package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wigo.client.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.List;

@Mixin(Screen.class)
public abstract class TooltipCaptureMixin {
    @Shadow protected Font font;
    @WrapMethod(method="renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;II)V")
    private void fit(PoseStack pose,List<? extends FormattedCharSequence> lines,int x,int y,Operation<Void> original) {
        if(GuiCapture.tooltipCanvas()==null||lines.isEmpty()) {original.call(pose,lines,x,y);return;}
        int width=0,height=lines.size()==1?8:lines.size()*10;
        for(var line:lines)width=Math.max(width,font.width(line));
        var screen=(Screen)(Object)this;
        int desiredX=x+12;if(desiredX+width>screen.width)desiredX-=28+width;
        int desiredY=y-12;if(desiredY+height+6>screen.height)desiredY=screen.height-height-6;
        int originX=12+width>screen.width?-16-width:12;
        int originY=-12+height+6>screen.height?screen.height-height-6:-12;
        LegacyTooltipLayout.draw(GuiGraphics.wrap(pose),width,height,desiredX,desiredY,originX,originY,
                ()->original.call(pose,lines,0,0));
    }
}
