package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wigo.client.PanelRenderer;
import dev.wigo.client.NameTagLayers;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SubmitNodeCollection.class)
public abstract class NameTagDepthMixin {
    @Shadow @Final public SimpleFeatureRenderPhase nameTags;

    @WrapOperation(method="submitNameTag",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/feature/phase/TranslucentFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/TranslucentSubmit;)V"))
    private void wigoNameTagDepth(TranslucentFeatureRenderPhase phase,TranslucentSubmit node,Operation<Void> original) {
        if(PanelRenderer.nameTagsNeedDepthTest()&&node instanceof TextFeatureRenderer.Submit text) {
            // Keep vanilla's text/background/brightness, but let the opaque panel occlude them.
            nameTags.submit(NameTagLayers.mark(text));
        } else original.call(phase,node);
    }

    @WrapOperation(method="submitNameTag",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/SubmitNodeCollection;submitNameTagPart(Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Submit;)V"))
    private void wigoDiscreteName(SubmitNodeCollection collection,TextFeatureRenderer.Submit text,Operation<Void> original) {
        if(PanelRenderer.nameTagsNeedDepthTest()&&text.content() instanceof TextFeatureRenderer.Content.Text content
                &&((content.color()>>>24)!=255||(content.backgroundColor()>>>24)!=0))text=NameTagLayers.mark(text);
        original.call(collection,text);
    }
}
