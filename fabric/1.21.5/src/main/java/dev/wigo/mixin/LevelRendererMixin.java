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
    @Inject(method="renderEntities",at=@At("TAIL"))
    private void wigoPanels(PoseStack pose,MultiBufferSource.BufferSource buffers,Camera camera,DeltaTracker delta,List<Entity> entities,CallbackInfo ci) {
        PanelRenderer.submit(pose,buffers,camera,delta);
    }
    @Inject(method="renderLevel",at=@At("TAIL"))
    private void wigoLatePanels(CallbackInfo ci) {
        try {PanelRenderer.renderClassicTransparency();}finally{PanelRenderer.finishClassicFrame();}
    }
}
