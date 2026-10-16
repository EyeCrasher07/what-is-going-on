package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wigo.client.NameTagLayers;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(TextFeatureRenderer.class)
public abstract class NameTagTextMixin {
    @WrapOperation(method="buildGroup",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/feature/TextFeatureRenderer;renderText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$GlyphRenderer;Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Content$Text;)V"))
    private static void wigoNameLayer(Font font,@Coerce Object glyphs,TextFeatureRenderer.Content.Text content,Operation<Void> original) {
        boolean previous=NameTagLayers.begin(content);
        try {original.call(font,glyphs,content);}
        finally {NameTagLayers.end(previous);}
    }
}
