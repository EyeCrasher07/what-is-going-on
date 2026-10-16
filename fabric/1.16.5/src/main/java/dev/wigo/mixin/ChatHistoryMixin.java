package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.wigo.client.ChatPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Move the fixed-function chat origin before vanilla applies its own chat scale. */
@Mixin(ChatComponent.class)
public abstract class ChatHistoryMixin {
    @WrapMethod(method="render")
    private void history(PoseStack pose,int ticks,Operation<Void> original) {
        RenderSystem.pushMatrix();
        try {RenderSystem.translatef(0,-offset(),0);original.call(pose,ticks);}
        finally {RenderSystem.popMatrix();}
    }
    @ModifyExpressionValue(method={"getClickedComponentStyleAt","handleChatQueueClicked"},at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"))
    private int hitTest(int height) {return height-offset();}
    private static int offset() {
        return Minecraft.getInstance().screen instanceof ChatScreen screen?ChatPreview.historyOffset(screen):0;
    }
}
