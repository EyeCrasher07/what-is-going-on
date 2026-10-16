package dev.wigo.mixin;

import dev.wigo.client.GuiRendererFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric API 1.21.6/7 only allows its registry to initialize once, for the main GUI. */
@Mixin(targets="net.fabricmc.fabric.impl.client.rendering.SpecialGuiElementRegistryImpl",remap=false)
public abstract class FabricGuiRegistryMixin {
    @Inject(method="onReady",at=@At("HEAD"),cancellable=true,remap=false)
    private static void wigoLocalVanillaRenderer(CallbackInfo ci) {
        // WIGO handles vanilla menus; leave the main GUI and its mod registrations alone.
        if (GuiRendererFactory.creatingOffscreen()) ci.cancel();
    }
}
