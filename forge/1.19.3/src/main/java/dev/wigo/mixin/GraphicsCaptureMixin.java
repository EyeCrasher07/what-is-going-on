package dev.wigo.mixin;
import dev.wigo.client.*;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Describe native primitives directly; no framebuffer readback or network pixels. */
@Mixin(GuiComponent.class)
public abstract class GraphicsCaptureMixin {
    @Inject(method="blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",at=@At("HEAD"),cancellable=true)
    private static void sprite(PoseStack pose,int x,int y,int z,int width,int height,TextureAtlasSprite sprite,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->GuiCapture.atlas(GuiGraphics.wrap(pose),sprite,0,0,0,0,x,y,width,height,-1));ci.cancel();}
    }
    @Inject(method="innerBlit(Lorg/joml/Matrix4f;IIIIIFFFF)V",at=@At("HEAD"),cancellable=true)
    private static void texture(Matrix4f pose,int x0,int x1,int y0,int y1,int z,float u0,float u1,float v0,float v1,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->GuiCapture.texture(GuiGraphics.matrix(pose),GuiGraphics.captureTexture,x0,y0,x1,y1,u0,u1,v0,v1,-1));ci.cancel();}
    }
    @Inject(method="fill(Lcom/mojang/blaze3d/vertex/PoseStack;IIIII)V",at=@At("HEAD"),cancellable=true)
    private static void rectangle(PoseStack pose,int x0,int y0,int x1,int y1,int color,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.rectangle(GuiGraphics.wrap(pose),x0,y0,x1,y1,color,color);ci.cancel();}
    }
    @Inject(method="fillGradient(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIIII)V",at=@At("HEAD"),cancellable=true)
    private static void gradient(Matrix4f pose,BufferBuilder buffers,int x0,int y0,int x1,int y1,int z,int top,int bottom,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.rectangle(GuiGraphics.matrix(pose),x0,y0,x1,y1,top,bottom);ci.cancel();}
    }
    @Inject(method="enableScissor",at=@At("HEAD"),cancellable=true)
    private static void scissor(int x0,int y0,int x1,int y1,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.scissor(GuiGraphics.wrap(new PoseStack()),x0,y0,x1,y1);ci.cancel();}
    }
    @Inject(method="disableScissor",at=@At("HEAD"),cancellable=true)
    private static void scissor(CallbackInfo ci) {if(GuiCapture.recording()) {GuiCapture.unscissor();ci.cancel();}}
}
