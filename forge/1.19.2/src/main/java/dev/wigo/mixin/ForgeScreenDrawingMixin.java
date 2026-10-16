package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wigo.client.LegacyScreenDrawing;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;

/** Both Forge and OptiFine enter this hook, including Forge's GUI events. */
@Mixin(value=ForgeHooksClient.class, remap=false)
public abstract class ForgeScreenDrawingMixin {
    @WrapMethod(method="drawScreen", remap=false)
    private static void draw(Screen screen, PoseStack pose, int x, int y, float delta, Operation<Void> original) {
        LegacyScreenDrawing.draw(screen, pose, x, y, delta, () -> original.call(screen,pose,x,y,delta));
    }
}
