package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.matrix.MatrixStack;
import dev.wigo.client.*;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.IReorderingProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.List;

@Mixin(Screen.class)
public abstract class TooltipCaptureMixin {
    @Shadow protected FontRenderer font;
    @WrapMethod(method="renderTooltip(Lcom/mojang/blaze3d/matrix/MatrixStack;Ljava/util/List;II)V")
    private void fit(MatrixStack pose,List<? extends IReorderingProcessor> lines,int x,int y,Operation<Void> original) {
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
    /** Forge item/component tooltips bypass vanilla's formatted-line overload. */
    @WrapMethod(method="renderWrappedToolTip",remap=false)
    private void fitComponents(MatrixStack pose,List<? extends net.minecraft.util.text.ITextProperties> lines,
                               int x,int y,FontRenderer tooltipFont,Operation<Void> original) {
        if(GuiCapture.tooltipCanvas()==null||lines.isEmpty()) {original.call(pose,lines,x,y,tooltipFont);return;}
        // Measure the native Forge layout at the neutral anchor used below.
        // GuiUtils wraps only when its text would cross the source window.
        int width=0,count=lines.size(),titleLines=1;
        for(var line:lines)width=Math.max(width,tooltipFont.width(line));
        if(width+16>((Screen)(Object)this).width) {
            int limit=Math.max(1,((Screen)(Object)this).width-16);width=0;count=0;
            for(int i=0;i<lines.size();i++) {
                var wrapped=tooltipFont.getSplitter().splitLines(lines.get(i),limit,net.minecraft.util.text.Style.EMPTY);
                if(i==0)titleLines=wrapped.size();
                count+=wrapped.size();
                for(var line:wrapped)width=Math.max(width,tooltipFont.width(line));
            }
        }
        int height=8+Math.max(0,count-1)*10+(count>titleLines?2:0);
        var screen=(Screen)(Object)this;
        int desiredX=x+12;if(desiredX+width+4>screen.width)desiredX=x-16-width;
        int desiredY=y-12;if(desiredY<4)desiredY=4;else if(desiredY+height+4>screen.height)desiredY=screen.height-height-4;
        LegacyTooltipLayout.draw(GuiGraphics.wrap(pose),width,height,desiredX,desiredY,12,4,
                ()->original.call(pose,lines,0,0,tooltipFont));
    }
}
