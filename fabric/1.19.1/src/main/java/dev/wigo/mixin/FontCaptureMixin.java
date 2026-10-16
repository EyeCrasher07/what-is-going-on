package dev.wigo.mixin;

import dev.wigo.client.GuiCapture;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.mojang.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cover both immediate text and the old string overload with a bidi flag. */
@Mixin(Font.class)
public abstract class FontCaptureMixin {
    @Inject(method="drawInternal(Ljava/lang/String;FFILcom/mojang/math/Matrix4f;ZZ)I",at=@At("HEAD"),cancellable=true)
    private void immediate(String text,float x,float y,int color,Matrix4f pose,boolean shadow,boolean bidi,CallbackInfoReturnable<Integer> ci) {
        capture(text==null?"":text,x,y,color,shadow,pose,0,ci);
    }
    @Inject(method="drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFILcom/mojang/math/Matrix4f;Z)I",at=@At("HEAD"),cancellable=true)
    private void immediate(FormattedCharSequence text,float x,float y,int color,Matrix4f pose,boolean shadow,CallbackInfoReturnable<Integer> ci) {
        capture(text,x,y,color,shadow,pose,0,ci);
    }
    @Inject(method="drawInBatch(Ljava/lang/String;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZIIZ)I",at=@At("HEAD"),cancellable=true)
    private void batch(String text,float x,float y,int color,boolean shadow,Matrix4f pose,MultiBufferSource buffers,boolean mode,int background,int light,boolean bidi,CallbackInfoReturnable<Integer> ci) {
        capture(text,x,y,color,shadow,pose,background,ci);
    }
    @Inject(method="drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I",at=@At("HEAD"),cancellable=true)
    private void batch(FormattedCharSequence text,float x,float y,int color,boolean shadow,Matrix4f pose,MultiBufferSource buffers,boolean mode,int background,int light,CallbackInfoReturnable<Integer> ci) {
        capture(text,x,y,color,shadow,pose,background,ci);
    }
    private void capture(String text,float x,float y,int color,boolean shadow,Matrix4f pose,int background,CallbackInfoReturnable<Integer> ci) {
        if(!GuiCapture.recording())return;
        capture(Component.literal(text).getVisualOrderText(),x,y,color,shadow,pose,background,ci);
    }
    private void capture(FormattedCharSequence text,float x,float y,int color,boolean shadow,Matrix4f pose,int background,CallbackInfoReturnable<Integer> ci) {
        if(!GuiCapture.recording())return;
        GuiCapture.record(()->GuiCapture.text(pose,text,x,y,color,shadow,background));
        ci.setReturnValue((int)x+((Font)(Object)this).width(text)+(shadow?1:0));
    }
}
