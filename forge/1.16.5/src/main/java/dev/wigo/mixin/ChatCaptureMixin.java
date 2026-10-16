package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wigo.client.GuiCapture;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatScreen.class)
public abstract class ChatCaptureMixin {
    @Shadow protected TextFieldWidget input;
    @WrapMethod(method="render")
    private void draft(MatrixStack g,int x,int y,float delta,Operation<Void> original) {
        GuiCapture.muted++;
        try { original.call(g,x,y,delta); } finally { GuiCapture.muted--; }
        GuiCapture.chat(input.getValue());
    }
}
