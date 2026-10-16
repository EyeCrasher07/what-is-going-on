package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.matrix.MatrixStack;
import dev.wigo.client.NameTagLayers;
import dev.wigo.client.PanelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EntityRenderer.class)
public abstract class NameTagRenderMixin {
    @WrapMethod(method="renderNameTag")
    private void wigoName(Entity state,ITextComponent text,MatrixStack pose,IRenderTypeBuffer buffers,int light,Operation<Void> original) {
        if(dev.wigo.client.GuiViewport.width()>0)return;
        boolean previous=PanelRenderer.beginNameTags(state,Minecraft.getInstance().gameRenderer.getMainCamera());
        try {original.call(state,text,pose,PanelRenderer.nameTagsNeedDepthTest()?NameTagLayers.buffer():buffers,light);}
        finally{PanelRenderer.endNameTags(previous);}
    }
    @ModifyArg(method="renderNameTag",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/FontRenderer;drawInBatch(Lnet/minecraft/util/text/ITextComponent;FFIZLnet/minecraft/util/math/vector/Matrix4f;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ZII)I"),index=7)
    private boolean wigoDepth(boolean mode) {return PanelRenderer.nameTagsNeedDepthTest()?false:mode;}
}
