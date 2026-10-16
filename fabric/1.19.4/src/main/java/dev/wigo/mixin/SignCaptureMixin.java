package dev.wigo.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wigo.client.*;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignEditScreen.class)
public abstract class SignCaptureMixin {
    @Inject(method="renderSignBackground",at=@At("HEAD"),cancellable=true)
    private void sign(PoseStack pose,MultiBufferSource.BufferSource buffers,BlockState state,CallbackInfo ci) {
        if(!GuiCapture.recording())return;
        GuiCapture.record(()->{
            String texture="minecraft:textures/entity/signs/"+SignBlock.getWoodType(state.getBlock()).name()+".png";
            var matrix=GuiCapture.pose(pose.last().pose());
            var bounds=LegacyBounds.transform(new ScreenRectangle(-47,-24,94,102),pose.last().pose());
            GuiCapture.model(GuiCapture.command("l",matrix,GuiCapture.clip(),texture,state.getBlock() instanceof StandingSignBlock),bounds);
        });
        ci.cancel();
    }
}
