package dev.wigo.mixin;

import dev.wigo.client.NameTagLayers;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A marked name must not reuse the previous ordinary name's prepared draw. */
@Mixin(targets="net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer$Group")
public abstract class NameTagGroupMixin {
    @Shadow private RenderType lastRenderType;
    @Unique private int wigoLayer;
    @Inject(method="getVertexBuilder",at=@At("HEAD"))
    private void wigoSeparateLayer(RenderType type,CallbackInfoReturnable<VertexConsumer> cir) {
        int layer=NameTagLayers.layer();
        if(layer!=wigoLayer)lastRenderType=null;
        wigoLayer=layer;
    }
}
