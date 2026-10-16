package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.wigo.client.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EditBox.class)
public abstract class EditBoxCaretMixin {
    @Shadow(remap=false) private boolean textShadow;
    @Shadow @Final private Font font;
    @Shadow private String value;
    @Shadow private int displayPos,cursorPos,textColor,textColorUneditable,maxLength;
    @Shadow private boolean isEditable,bordered;
    @Shadow private long focusedTime;
    @Shadow private java.util.function.BiFunction<String,Integer,FormattedCharSequence> formatter;
    @Unique private boolean wigoCaptureCaret;
    @Unique private long wigoFocus=Long.MIN_VALUE,wigoEpoch;
    @WrapMethod(method="renderWidget")
    private void wigoCaret(GuiGraphics g,int mouseX,int mouseY,float delta,Operation<Void> original) {
        boolean previous=wigoCaptureCaret;wigoCaptureCaret=GuiCapture.active()&&WigoClient.compactMode();
        try {
            original.call(g,mouseX,mouseY,delta);
            EditBox self=(EditBox)(Object)this;
            if(!wigoCaptureCaret||!GuiCapture.active()||!self.isVisible()||!self.isFocused())return;
            String shown=font.plainSubstrByWidth(value.substring(displayPos),self.getInnerWidth());int position=cursorPos-displayPos;
            if(position<0||position>shown.length())return;
            int textY=self.getY()+(bordered?(self.getHeight()-8)/2:0);int x=self.getX()+(bordered?4:0);if(!shown.isEmpty())x+=font.width(formatter.apply(shown.substring(0,position),displayPos))+1;
            boolean insert=cursorPos<value.length()||value.length()>=maxLength;if(insert)x--;
            if(wigoFocus!=focusedTime){wigoFocus=focusedTime;wigoEpoch=WigoClient.serverTime()-(Util.getMillis()-focusedTime);}
            GuiCapture.caret(g,x,textY,isEditable?textColor:textColorUneditable,insert,textShadow,wigoEpoch);
        }finally {wigoCaptureCaret=previous;}
    }
    @WrapOperation(method="renderWidget",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V"))
    private void wigoInsert(GuiGraphics g,net.minecraft.client.renderer.RenderType type,int x0,int y0,int x1,int y1,int color,Operation<Void> original) {
        if(wigoCaptureCaret)GuiCapture.muted++;
        try{original.call(g,type,x0,y0,x1,y1,color);}finally{if(wigoCaptureCaret)GuiCapture.muted--;}
    }
    @WrapOperation(method="renderWidget",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"))
    private int wigoAppend(GuiGraphics g,Font font,String text,int x,int y,int color,boolean shadow,Operation<Integer> original) {
        boolean caret=wigoCaptureCaret&&text.equals("_");if(caret)GuiCapture.muted++;
        try{return original.call(g,font,text,x,y,color,shadow);}finally{if(caret)GuiCapture.muted--;}
    }
}
