package dev.wigo.mixin;

import dev.wigo.client.GuiCapture;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.network.chat.Component;

@Mixin(Font.class)
public abstract class FontCaptureMixin {
    @Inject(method="drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",at=@At("HEAD"),cancellable=true)
    private void wigoText(String text,float x,float y,int color,boolean shadow,Matrix4f pose,MultiBufferSource buffers,Font.DisplayMode mode,int background,int light,CallbackInfoReturnable<Integer> ci) {
        if(GuiCapture.recording()) {
            GuiCapture.record(()->GuiCapture.text(pose,Component.literal(text).getVisualOrderText(),x,y,color,shadow,background));
            ci.setReturnValue((int)x+((Font)(Object)this).width(text)+(shadow?1:0));
        }
    }
    @Inject(method="drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",at=@At("HEAD"),cancellable=true)
    private void wigoText(FormattedCharSequence text,float x,float y,int color,boolean shadow,Matrix4f pose,MultiBufferSource buffers,Font.DisplayMode mode,int background,int light,CallbackInfoReturnable<Integer> ci) {
        if(GuiCapture.recording()) {
            GuiCapture.record(()->GuiCapture.text(pose,text,x,y,color,shadow,background));
            ci.setReturnValue((int)x+((Font)(Object)this).width(text)+(shadow?1:0));
        }
    }
    @Inject(method="drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",at=@At("HEAD"),cancellable=true)
    private void wigoComponent(Component text,float x,float y,int color,boolean shadow,Matrix4f pose,MultiBufferSource buffers,Font.DisplayMode mode,int background,int light,CallbackInfoReturnable<Integer> ci) {
        if(GuiCapture.recording()) {
            GuiCapture.record(()->GuiCapture.text(pose,text.getVisualOrderText(),x,y,color,shadow,background));
            ci.setReturnValue((int)x+((Font)(Object)this).width(text)+(shadow?1:0));
        }
    }
    @Inject(method="drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I",at=@At("HEAD"),cancellable=true,require=0)
    private void wigoComponent(Component text,float x,float y,int color,boolean shadow,Matrix4f pose,MultiBufferSource buffers,Font.DisplayMode mode,int background,int light,boolean inverseDepth,CallbackInfoReturnable<Integer> ci) {
        wigoComponent(text,x,y,color,shadow,pose,buffers,mode,background,light,ci);
    }

}
