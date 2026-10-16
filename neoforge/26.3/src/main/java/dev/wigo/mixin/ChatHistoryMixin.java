package dev.wigo.mixin;

import dev.wigo.client.ChatPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChatComponent.class)
public abstract class ChatHistoryMixin {
    @ModifyArg(method={
            "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            "captureClickableText"
    },at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/ChatComponent;extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V"),index=1)
    private int wigoHistoryHeight(int height) {
        if (Minecraft.getInstance().gui.screen() instanceof ChatScreen screen) return height-ChatPreview.historyOffset(screen);
        return height;
    }
}
