package dev.wigo.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wigo.client.PanelRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @org.spongepowered.asm.mixin.Unique private boolean wigoWorldDepth;
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="addMainPass",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V"))
    private void wigoSaveDepth(com.mojang.blaze3d.framegraph.FramePass pass,Runnable render,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        // Snapshot after the main pass actually runs, before post-processing.
        original.call(pass,(Runnable)()->{render.run();wigoWorldDepth=PanelRenderer.preserveWorldDepth();});
    }

    @Inject(method="renderEntities",at=@At("TAIL"))
    private void wigoPanels(PoseStack pose,MultiBufferSource.BufferSource buffers,Camera camera,DeltaTracker delta,List<Entity> entities,CallbackInfo ci) {
        PanelRenderer.submit(pose,buffers,camera,delta);
    }
    @Inject(method="renderLevel",at=@At("TAIL"))
    private void wigoLatePanels(CallbackInfo ci) {
        try {if(wigoWorldDepth)PanelRenderer.restoreWorldDepth();PanelRenderer.renderClassicTransparency();}finally{wigoWorldDepth=false;PanelRenderer.finishClassicFrame();}
    }
}
