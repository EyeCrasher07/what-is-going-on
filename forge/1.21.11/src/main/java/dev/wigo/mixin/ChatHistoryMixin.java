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
            "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V",
            "captureClickableText"
    },at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/ChatComponent;render(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IIZ)V"),index=1)
    private int historyHeight(int height) {
        if(Minecraft.getInstance().screen instanceof ChatScreen screen)return height-ChatPreview.historyOffset(screen);
        return height;
    }
}
