package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wigo.client.GuiCapture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatScreen.class)
public abstract class ChatCaptureMixin {
    @Shadow protected EditBox input;
    @WrapMethod(method="extractRenderState")
    private void draft(GuiGraphicsExtractor g,int x,int y,float delta,Operation<Void> original) {
        GuiCapture.muted++;
        try { original.call(g,x,y,delta); } finally { GuiCapture.muted--; }
        GuiCapture.chat(input.getValue());
    }
}
