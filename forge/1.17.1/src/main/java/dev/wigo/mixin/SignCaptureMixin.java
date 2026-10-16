package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.vertex.*;
import dev.wigo.client.*;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SignEditScreen.class)
public abstract class SignCaptureMixin {
    @Shadow @Final private SignBlockEntity sign;
    @Shadow private WoodType woodType;
    @WrapOperation(method="render", at=@At(value="INVOKE", target="Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void model(ModelPart part, PoseStack pose, VertexConsumer vertices, int light, int overlay, Operation<Void> original) {
        if (!GuiCapture.recording()) { original.call(part, pose, vertices, light, overlay); return; }
        GuiCapture.record(() -> {
            // The normalized vanilla sign model occupies [-0.75,0.75] x [-0.3125,1.125].
            var matrix = GuiCapture.pose(pose.last().pose());
            var local = new org.joml.Matrix3x2f(matrix).scale(1/62.500004f,1/62.500004f).translate(0,-31);
            var actual = LegacyBounds.transform(new ScreenRectangle(-47,-24,94,102), LegacyMatrices.affine(local.m00(),local.m01(),local.m10(),local.m11(),local.m20(),local.m21()));
            GuiCapture.model(GuiCapture.command("l",local,GuiCapture.clip(),"minecraft:textures/entity/signs/"+woodType.name()+".png",sign.getBlockState().getBlock() instanceof StandingSignBlock),actual);
        });
    }
}
