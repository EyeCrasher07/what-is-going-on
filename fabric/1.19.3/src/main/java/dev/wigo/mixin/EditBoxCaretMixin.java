package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wigo.client.*;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EditBox.class)
public abstract class EditBoxCaretMixin {
    @Shadow @Final private Font font;
    @Shadow private String value;
    @Shadow private int displayPos,cursorPos,textColor,textColorUneditable,maxLength,frame;
    @Shadow private boolean isEditable,bordered;
    @Shadow private java.util.function.BiFunction<String,Integer,FormattedCharSequence> formatter;
    @Unique private boolean wigoCaptureCaret,wigoFocused;
    @Unique private int wigoLastFrame;
    @Unique private long wigoEpoch;
    @WrapMethod(method="renderButton")
    private void wigoCaret(PoseStack pose,int mouseX,int mouseY,float delta,Operation<Void> original) {
        boolean previous=wigoCaptureCaret;wigoCaptureCaret=GuiCapture.active()&&WigoClient.compactMode();
        try {
            original.call(pose,mouseX,mouseY,delta);
            EditBox self=(EditBox)(Object)this;
            if(!self.isFocused()){wigoFocused=false;return;}
            if(!wigoCaptureCaret||!GuiCapture.active()||!self.isVisible())return;
            String shown=font.plainSubstrByWidth(value.substring(displayPos),self.getInnerWidth());int position=cursorPos-displayPos;
            if(position<0||position>shown.length())return;
            int x=self.getX()+(bordered?4:0),y=self.getY()+(bordered?(self.getHeight()-8)/2:0);
            if(!shown.isEmpty())x+=font.width(formatter.apply(shown.substring(0,position),displayPos))+1;
            boolean insert=cursorPos<value.length()||value.length()>=maxLength;if(insert)x--;
            if(!wigoFocused||frame<wigoLastFrame)wigoEpoch=WigoClient.serverTime()-frame*50L;
            wigoFocused=true;wigoLastFrame=frame;
            GuiCapture.caret(GuiGraphics.wrap(pose),x,y,isEditable?textColor:textColorUneditable,insert,true,wigoEpoch);
        }finally {wigoCaptureCaret=previous;}
    }
    @WrapOperation(method="renderButton",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiComponent;fill(Lcom/mojang/blaze3d/vertex/PoseStack;IIIII)V"))
    private void wigoInsert(PoseStack pose,int x0,int y0,int x1,int y1,int color,Operation<Void> original) {
        if(wigoCaptureCaret)GuiCapture.muted++;
        try{original.call(pose,x0,y0,x1,y1,color);}finally{if(wigoCaptureCaret)GuiCapture.muted--;}
    }
    @WrapOperation(method="renderButton",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;drawShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFI)I"))
    private int wigoAppend(Font font,PoseStack pose,String text,float x,float y,int color,Operation<Integer> original) {
        boolean caret=wigoCaptureCaret&&text.equals("_");if(caret)GuiCapture.muted++;
        try{return original.call(font,pose,text,x,y,color);}finally{if(caret)GuiCapture.muted--;}
    }
}
