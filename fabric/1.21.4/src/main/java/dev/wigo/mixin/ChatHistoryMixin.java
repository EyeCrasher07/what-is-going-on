package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.wigo.client.ChatPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Shift the native chat baseline and its mouse transform by the same GUI distance. */
@Mixin(ChatComponent.class)
public abstract class ChatHistoryMixin {
    @ModifyExpressionValue(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;guiHeight()I"))
    private int historyHeight(int height) { return height-offset(); }

    @ModifyExpressionValue(method="screenToChatY",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"))
    private int hitTestHeight(int height) { return height-offset(); }

    private static int offset() {
        return Minecraft.getInstance().screen instanceof ChatScreen screen?ChatPreview.historyOffset(screen):0;
    }
}
