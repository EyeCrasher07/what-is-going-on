package dev.wigo.mixin;
import dev.wigo.client.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.BannerTileEntityRenderer;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.item.DyeColor;
import dev.wigo.client.LegacyBannerPatterns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BannerTileEntityRenderer.class)
public abstract class BannerCaptureMixin {
    @Inject(method="renderPatterns(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;IILnet/minecraft/client/renderer/model/ModelRenderer;Lnet/minecraft/client/renderer/model/RenderMaterial;ZLjava/util/List;Z)V",at=@At("HEAD"),cancellable=true)
    private static void wigoBanner(MatrixStack pose,IRenderTypeBuffer buffers,int light,int overlay,ModelRenderer flag,RenderMaterial material,boolean banner,java.util.List<com.mojang.datafixers.util.Pair<net.minecraft.tileentity.BannerPattern,DyeColor>> patterns,boolean glint,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->LegacyCapture.banner(pose.last().pose(),flag,patterns.isEmpty()?DyeColor.WHITE:patterns.get(0).getSecond(),new LegacyBannerPatterns(patterns)));ci.cancel();}
    }
}
