package dev.wigo.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wigo.client.NameTagLayers;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
@Mixin(NameTagFeatureRenderer.class)
public abstract class NameTagTextMixin {
    @WrapOperation(method="buildGroup",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/feature/NameTagFeatureRenderer;prepareText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/renderer/feature/NameTagFeatureRenderer$Submit;)Lnet/minecraft/client/gui/Font$PreparedText;"))
    private static Font.PreparedText wigoPreparedName(Font font,NameTagFeatureRenderer.Submit node,Operation<Font.PreparedText> original) {
        var result=original.call(font,node);NameTagLayers.prepared(node,result);return result;
    }
    @WrapOperation(method="buildGroup",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font$PreparedText;visit(Lnet/minecraft/client/gui/Font$GlyphVisitor;)V"))
    private void wigoNameLayer(Font.PreparedText text,@Coerce Object visitor,Operation<Void> original) {
        boolean previous=NameTagLayers.begin(text);
        Font.GlyphVisitor delegate=(Font.GlyphVisitor)visitor;
        try{original.call(text,new Font.GlyphVisitor() {
            @Override public void acceptGlyph(TextRenderable.Styled glyph) {
                boolean old=NameTagLayers.effect(false);try{delegate.acceptGlyph(glyph);}finally{NameTagLayers.effect(old);}
            }
            @Override public void acceptEffect(TextRenderable renderable) {
                boolean old=NameTagLayers.effect(true);try{delegate.acceptEffect(renderable);}finally{NameTagLayers.effect(old);}
            }
            @Override public void acceptRenderable(TextRenderable renderable) {delegate.acceptRenderable(renderable);}
        });}finally{NameTagLayers.end(previous);}
    }
}
