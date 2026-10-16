package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wigo.client.PanelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityRenderer.class)
public abstract class NameTagRenderMixin {
    @WrapMethod(method="submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V")
    private void wigoNameTags(EntityRenderState state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera,int offset,Operation<Void> original) {
        if(dev.wigo.client.GuiViewport.width()>0)return;
        boolean previous=PanelRenderer.beginNameTags(state,camera);
        try {original.call(state,pose,collector,camera,offset);}
        finally {PanelRenderer.endNameTags(previous);}
    }
}
