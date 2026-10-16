package dev.wigo.mixin;
import dev.wigo.client.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.item.DyeColor;
import dev.wigo.client.LegacyBannerPatterns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BannerRenderer.class)
public abstract class BannerCaptureMixin {
    @Inject(method="renderPatterns(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;ZLjava/util/List;Z)V",at=@At("HEAD"),cancellable=true)
    private static void wigoBanner(PoseStack pose,MultiBufferSource buffers,int light,int overlay,ModelPart flag,Material material,boolean banner,java.util.List<com.mojang.datafixers.util.Pair<net.minecraft.core.Holder<net.minecraft.world.level.block.entity.BannerPattern>,DyeColor>> patterns,boolean glint,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->LegacyCapture.banner(pose.last().pose(),flag,patterns.isEmpty()?DyeColor.WHITE:patterns.get(0).getSecond(),new LegacyBannerPatterns(patterns)));ci.cancel();}
    }
}
