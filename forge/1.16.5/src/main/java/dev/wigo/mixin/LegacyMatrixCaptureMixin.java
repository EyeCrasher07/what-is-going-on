package dev.wigo.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.vector.Matrix4f;
import dev.wigo.client.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public abstract class LegacyMatrixCaptureMixin {
    @Inject(method="color4f",at=@At("HEAD")) private static void color(float r,float g,float b,float a,CallbackInfo ci) {LegacyGlMatrices.color(r,g,b,a);}
    @Inject(method="enableTexture",at=@At("HEAD")) private static void enableTexture(CallbackInfo ci) {LegacyGlMatrices.texture(true);}
    @Inject(method="disableTexture",at=@At("HEAD")) private static void disableTexture(CallbackInfo ci) {LegacyGlMatrices.texture(false);}
    @Inject(method="matrixMode",at=@At("HEAD")) private static void mode(int value,CallbackInfo ci) {LegacyGlMatrices.mode(value);}
    @Inject(method="loadIdentity",at=@At("HEAD")) private static void identity(CallbackInfo ci) {LegacyGlMatrices.identity();}
    @Inject(method="pushMatrix",at=@At("HEAD")) private static void push(CallbackInfo ci) {LegacyGlMatrices.push();}
    @Inject(method="popMatrix",at=@At("HEAD")) private static void pop(CallbackInfo ci) {LegacyGlMatrices.pop();}
    @Inject(method="translatef",at=@At("HEAD")) private static void translate(float x,float y,float z,CallbackInfo ci) {LegacyGlMatrices.translate(x,y,z);}
    @Inject(method="translated",at=@At("HEAD")) private static void translated(double x,double y,double z,CallbackInfo ci) {LegacyGlMatrices.translate((float)x,(float)y,(float)z);}
    @Inject(method="scalef",at=@At("HEAD")) private static void scale(float x,float y,float z,CallbackInfo ci) {LegacyGlMatrices.scale(x,y,z);}
    @Inject(method="scaled",at=@At("HEAD")) private static void scaled(double x,double y,double z,CallbackInfo ci) {LegacyGlMatrices.scale((float)x,(float)y,(float)z);}
    @Inject(method="rotatef",at=@At("HEAD")) private static void rotate(float angle,float x,float y,float z,CallbackInfo ci) {LegacyGlMatrices.rotate(angle,x,y,z);}
    @Inject(method="multMatrix",at=@At("HEAD")) private static void multiply(Matrix4f matrix,CallbackInfo ci) {LegacyGlMatrices.multiply(matrix);}
    @Inject(method="clear",at=@At("HEAD"),cancellable=true) private static void clear(int mask,boolean check,CallbackInfo ci) {if(GuiCapture.recording())ci.cancel();}
}
