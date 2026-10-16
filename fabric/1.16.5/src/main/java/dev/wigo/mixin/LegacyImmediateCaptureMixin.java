package dev.wigo.mixin;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.wigo.client.GuiCapture;
import dev.wigo.client.LegacyImmediateGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferUploader.class)
public abstract class LegacyImmediateCaptureMixin {
    @Inject(method = "end", at = @At("HEAD"), cancellable = true)
    private static void instructions(BufferBuilder buffer, CallbackInfo ci) {
        if (!GuiCapture.recording()) return;
        // OptiFine bypasses vanilla's _end helper. Consume the same draw state
        // at the common entry point before either implementation submits it.
        var draw=buffer.popNextBuffer();
        var state=draw.getFirst();
        GuiCapture.record(() -> LegacyImmediateGui.record(draw.getSecond(), state.mode(), state.format(), state.vertexCount()));
        ci.cancel();
    }
}
