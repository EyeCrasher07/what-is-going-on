package dev.wigo.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.commands.RenderPass;
import dev.wigo.client.PanelRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Collect WIGO geometry through Minecraft's renderer on every loader. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method="submitEntities",at=@At("TAIL"))
    private void wigoPanels(PoseStack pose,LevelRenderState state,SubmitNodeCollector collector,CallbackInfo ci) {
        PanelRenderer.submit(pose,state,collector);
    }

    @Inject(method="executeClassicTransparency",at=@At("TAIL"))
    private void wigoTransparentPanels(ChunkSectionsToRender chunks,FeatureRenderDispatcher.PreparedFrame features,RenderPass pass,CallbackInfo ci) {
        PanelRenderer.renderClassicTransparency(pass);
    }

    @Inject(method="render",at=@At("TAIL"))
    private void wigoFinishPanels(CallbackInfo ci) {
        PanelRenderer.finishClassicFrame();
    }
}
