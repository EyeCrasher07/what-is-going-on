package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wigo.client.NameTagLayers;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NameTagFeatureRenderer.class)
public abstract class NameTagTextMixin {
    @WrapOperation(method="renderTranslucent",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4fc;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"))
    private void wigoLateName(Font font,Component text,float x,float y,int color,boolean shadow,Matrix4fc pose,MultiBufferSource buffers,Font.DisplayMode mode,int background,int light,Operation<Void> original) {
        if(NameTagLayers.contains(pose)) {buffers=NameTagLayers.buffer();mode=Font.DisplayMode.NORMAL;}
        original.call(font,text,x,y,color,shadow,pose,buffers,mode,background,light);
    }
}
