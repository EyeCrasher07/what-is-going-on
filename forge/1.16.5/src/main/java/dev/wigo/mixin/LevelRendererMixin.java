package dev.wigo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import dev.wigo.client.PanelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.profiler.IProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(WorldRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow private ShaderGroup transparencyChain;
    @WrapOperation(method="renderLevel",slice=@Slice(from=@At(value="CONSTANT",args="stringValue=blockentities"),to=@At(value="CONSTANT",args="stringValue=destroyProgress")),
            at=@At(value="INVOKE",target="Lnet/minecraft/profiler/IProfiler;popPush(Ljava/lang/String;)V",ordinal=0))
    private void menus(IProfiler profiler,String phase,Operation<Void> original,@Local MatrixStack pose,
                       @Local(argsOnly=true) ActiveRenderInfo camera,@Local(argsOnly=true) float delta) {
        if(phase.equals("blockentities"))PanelRenderer.submit(pose,Minecraft.getInstance().renderBuffers().bufferSource(),camera,delta);
        original.call(profiler,phase);
    }
    @Inject(method="renderLevel",at=@At("TAIL"))
    private void wigoLatePanels(CallbackInfo ci) {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        try {PanelRenderer.renderClassicTransparency();}finally{PanelRenderer.finishClassicFrame();}
    }
    @WrapOperation(method="renderLevel",at=@At(value="INVOKE",target="Lnet/minecraft/client/shader/ShaderGroup;process(F)V"))
    private void wigoKeepWorldDepth(ShaderGroup chain,float delta,Operation<Void> original) {
        boolean saved=chain==transparencyChain&&PanelRenderer.preserveWorldDepth();
        try {original.call(chain,delta);}finally{if(saved)PanelRenderer.restoreWorldDepth();}
    }
}
