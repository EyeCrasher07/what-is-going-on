package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wigo.client.NameTagLayers;
import dev.wigo.client.PanelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EntityRenderer.class)
public abstract class NameTagRenderMixin {
    @WrapMethod(method="renderNameTag")
    private void wigoName(EntityRenderState state,Component text,PoseStack pose,MultiBufferSource buffers,int light,Operation<Void> original) {
        if(dev.wigo.client.GuiViewport.width()>0)return;
        boolean previous=PanelRenderer.beginNameTags(state,Minecraft.getInstance().gameRenderer.getMainCamera());
        try {original.call(state,text,pose,PanelRenderer.nameTagsNeedDepthTest()?NameTagLayers.buffer():buffers,light);}
        finally{PanelRenderer.endNameTags(previous);}
    }
    @ModifyArg(method="renderNameTag",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I"),index=7)
    private Font.DisplayMode wigoDepth(Font.DisplayMode mode) {return PanelRenderer.nameTagsNeedDepthTest()?Font.DisplayMode.NORMAL:mode;}
}
