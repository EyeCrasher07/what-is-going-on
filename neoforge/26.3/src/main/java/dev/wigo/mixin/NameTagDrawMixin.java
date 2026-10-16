package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.commands.RenderPass;
import dev.wigo.client.NameTagLayers;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderTypeFeatureRenderer.class)
public abstract class NameTagDrawMixin {
    @WrapOperation(method="executeGroup",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/rendertype/PreparedRenderType;drawFromBuffer(Lnet/minecraft/client/renderer/StagedVertexBuffer$ExecuteInfo;Lcom/mojang/renderpearl/api/commands/RenderPass;)V"))
    private void wigoLateName(PreparedRenderType type,StagedVertexBuffer.ExecuteInfo info,RenderPass pass,Operation<Void> original) {
        if(!NameTagLayers.defer(type,info))original.call(type,info,pass);
    }
}
