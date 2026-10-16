package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.wigo.client.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.TextCursorUtils;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** Capture a stable caret description while leaving the owner's native blink intact. */
@Mixin(EditBox.class)
public abstract class EditBoxCaretMixin {
    @Shadow @Final private Font font;
    @Shadow private String value;
    @Shadow private int displayPos,cursorPos,textX,textY,textColor,textColorUneditable,maxLength;
    @Shadow private boolean isEditable,textShadow;
    @Shadow private long focusedTime;
    @Shadow private FormattedCharSequence applyFormat(String text,int offset){throw new AssertionError();}
    @Unique private boolean wigoCaptureCaret;
    @Unique private long wigoFocus=Long.MIN_VALUE,wigoEpoch;
    @WrapMethod(method="extractWidgetRenderState")
    private void wigoCaret(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta,Operation<Void> original) {
        boolean previous=wigoCaptureCaret;wigoCaptureCaret=GuiCapture.active()&&WigoClient.compactMode();
        try {
            original.call(graphics,mouseX,mouseY,delta);
            EditBox self=(EditBox)(Object)this;
            if(!wigoCaptureCaret||!GuiCapture.active()||!self.isVisible()||!self.isFocused())return;
            String shown=font.plainSubstrByWidth(value.substring(displayPos),self.getInnerWidth());int position=cursorPos-displayPos;
            if(position<0||position>shown.length())return;
            int x=textX;if(!shown.isEmpty())x+=font.width(applyFormat(shown.substring(0,position),displayPos))+1;
            boolean insert=cursorPos<value.length()||value.length()>=maxLength;if(insert)x--;
            if(wigoFocus!=focusedTime){wigoFocus=focusedTime;wigoEpoch=WigoClient.serverTime()-(Util.getMillis()-focusedTime);}
            GuiCapture.caret(graphics,x,textY,isEditable?textColor:textColorUneditable,insert,textShadow,wigoEpoch);
        }finally {wigoCaptureCaret=previous;}
    }
    @WrapOperation(method="extractWidgetRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/TextCursorUtils;extractInsertCursor(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIII)V"))
    private void wigoInsert(GuiGraphicsExtractor graphics,int x,int y,int color,int height,Operation<Void> original) {
        if(wigoCaptureCaret)GuiCapture.muted++;
        try{original.call(graphics,x,y,color,height);}finally {if(wigoCaptureCaret)GuiCapture.muted--;}
    }
    @WrapOperation(method="extractWidgetRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/TextCursorUtils;extractAppendCursor(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIIZ)V"))
    private void wigoAppend(GuiGraphicsExtractor graphics,Font font,int x,int y,int color,boolean shadow,Operation<Void> original) {
        if(wigoCaptureCaret)GuiCapture.muted++;
        try{original.call(graphics,font,x,y,color,shadow);}finally {if(wigoCaptureCaret)GuiCapture.muted--;}
    }
}
