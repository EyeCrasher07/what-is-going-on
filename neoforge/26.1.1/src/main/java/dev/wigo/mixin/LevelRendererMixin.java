package dev.wigo.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wigo.client.PanelRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method="submitEntities",at=@At("TAIL"))
    private void wigoPanels(PoseStack pose,LevelRenderState state,SubmitNodeCollector collector,CallbackInfo ci) {
        PanelRenderer.submit(pose,state,collector);
    }
    @Inject(method="renderLevel",at=@At("TAIL"))
    private void wigoLatePanels(CallbackInfo ci) {
        try {PanelRenderer.renderClassicTransparency();}finally{PanelRenderer.finishClassicFrame();}
    }
}
