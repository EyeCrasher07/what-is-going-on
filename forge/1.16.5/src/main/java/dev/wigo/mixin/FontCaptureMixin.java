package dev.wigo.mixin;

import dev.wigo.client.GuiCapture;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.vector.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cover both immediate text and the old string overload with a bidi flag. */
@Mixin(FontRenderer.class)
public abstract class FontCaptureMixin {
    @Inject(method="drawInternal(Ljava/lang/String;FFILnet/minecraft/util/math/vector/Matrix4f;ZZ)I",at=@At("HEAD"),cancellable=true)
    private void immediate(String text,float x,float y,int color,Matrix4f pose,boolean shadow,boolean bidi,CallbackInfoReturnable<Integer> ci) {
        capture(text==null?"":text,x,y,color,shadow,pose,0,ci);
    }
    @Inject(method="drawInternal(Lnet/minecraft/util/IReorderingProcessor;FFILnet/minecraft/util/math/vector/Matrix4f;Z)I",at=@At("HEAD"),cancellable=true)
    private void immediate(IReorderingProcessor text,float x,float y,int color,Matrix4f pose,boolean shadow,CallbackInfoReturnable<Integer> ci) {
        capture(text,x,y,color,shadow,pose,0,ci);
    }
    @Inject(method="drawInBatch(Ljava/lang/String;FFIZLnet/minecraft/util/math/vector/Matrix4f;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ZIIZ)I",at=@At("HEAD"),cancellable=true)
    private void batch(String text,float x,float y,int color,boolean shadow,Matrix4f pose,IRenderTypeBuffer buffers,boolean mode,int background,int light,boolean bidi,CallbackInfoReturnable<Integer> ci) {
        capture(text,x,y,color,shadow,pose,background,ci);
    }
    @Inject(method="drawInBatch(Lnet/minecraft/util/IReorderingProcessor;FFIZLnet/minecraft/util/math/vector/Matrix4f;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ZII)I",at=@At("HEAD"),cancellable=true)
    private void batch(IReorderingProcessor text,float x,float y,int color,boolean shadow,Matrix4f pose,IRenderTypeBuffer buffers,boolean mode,int background,int light,CallbackInfoReturnable<Integer> ci) {
        capture(text,x,y,color,shadow,pose,background,ci);
    }
    private void capture(String text,float x,float y,int color,boolean shadow,Matrix4f pose,int background,CallbackInfoReturnable<Integer> ci) {
        if(!GuiCapture.recording())return;
        capture(new net.minecraft.util.text.StringTextComponent(text).getVisualOrderText(),x,y,color,shadow,pose,background,ci);
    }
    private void capture(IReorderingProcessor text,float x,float y,int color,boolean shadow,Matrix4f pose,int background,CallbackInfoReturnable<Integer> ci) {
        if(!GuiCapture.recording())return;
        GuiCapture.record(()->GuiCapture.text(pose,text,x,y,color,shadow,background));
        ci.setReturnValue((int)x+((FontRenderer)(Object)this).width(text)+(shadow?1:0));
    }
}
