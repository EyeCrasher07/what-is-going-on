package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.wigo.client.*;
import net.minecraft.client.gui.screen.EditSignScreen;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.block.StandingSignBlock;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.block.WoodType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EditSignScreen.class)
public abstract class SignCaptureMixin {
    @Shadow @Final private SignTileEntity sign;
    @WrapOperation(method="render", at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/model/ModelRenderer;render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;II)V"))
    private void model(ModelRenderer part, MatrixStack pose, IVertexBuilder vertices, int light, int overlay, Operation<Void> original) {
        if (!GuiCapture.recording()) { original.call(part, pose, vertices, light, overlay); return; }
        GuiCapture.record(() -> {
            // The normalized vanilla sign model occupies [-0.75,0.75] x [-0.3125,1.125].
            var matrix = GuiCapture.pose(pose.last().pose());
            var local = new org.joml.Matrix3x2f(matrix).scale(1/62.500004f,1/62.500004f).translate(0,-31);
            var actual = LegacyBounds.transform(new ScreenRectangle(-47,-24,94,102), LegacyMatrices.affine(local.m00(),local.m01(),local.m10(),local.m11(),local.m20(),local.m21()));
            GuiCapture.model(GuiCapture.command("l",local,GuiCapture.clip(),"minecraft:textures/entity/signs/"+((net.minecraft.block.AbstractSignBlock)sign.getBlockState().getBlock()).type().name()+".png",sign.getBlockState().getBlock() instanceof StandingSignBlock),actual);
        });
    }
}
