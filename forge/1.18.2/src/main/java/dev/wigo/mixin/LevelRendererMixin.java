package dev.wigo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wigo.client.PanelRenderer;
import net.minecraft.client.*;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow private PostChain transparencyChain;
    @WrapOperation(method="renderLevel",slice=@Slice(from=@At(value="CONSTANT",args="stringValue=blockentities"),to=@At(value="CONSTANT",args="stringValue=destroyProgress")),
            at=@At(value="INVOKE",target="Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",ordinal=0))
    private void menus(ProfilerFiller profiler,String phase,Operation<Void> original,@Local PoseStack pose,
                       @Local(argsOnly=true) Camera camera,@Local(argsOnly=true) float delta) {
        if(phase.equals("blockentities"))PanelRenderer.submit(pose,Minecraft.getInstance().renderBuffers().bufferSource(),camera,delta);
        original.call(profiler,phase);
    }
    @Inject(method="renderLevel",at=@At("TAIL"))
    private void wigoLatePanels(CallbackInfo ci) {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        try {PanelRenderer.renderClassicTransparency();}finally{PanelRenderer.finishClassicFrame();}
    }
    @WrapOperation(method="renderLevel",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/PostChain;process(F)V"))
    private void wigoKeepWorldDepth(PostChain chain,float delta,Operation<Void> original) {
        boolean saved=chain==transparencyChain&&PanelRenderer.preserveWorldDepth();
        try {original.call(chain,delta);}finally{if(saved)PanelRenderer.restoreWorldDepth();}
    }
}
