package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.wigo.client.NameTagLayers;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderType.class)
public abstract class NameTagPipelineMixin {
    @ModifyReturnValue(method="prepare",at=@At("RETURN"))
    private PreparedRenderType wigoNamePipeline(PreparedRenderType type) {return NameTagLayers.prepare(type);}
}
