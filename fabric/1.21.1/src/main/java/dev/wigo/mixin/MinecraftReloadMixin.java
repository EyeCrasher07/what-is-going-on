package dev.wigo.mixin;

import dev.wigo.client.PanelRenderer;
import dev.wigo.client.WigoClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftReloadMixin {
    @Inject(method="reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;",at=@At("HEAD"))
    private void wigoReload(CallbackInfoReturnable<?> result) {
        Minecraft.getInstance().execute(PanelRenderer::close);

    }
    @Inject(method="reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;",at=@At("RETURN"))
    private void wigoReloaded(CallbackInfoReturnable<java.util.concurrent.CompletableFuture<Void>> result) {
        result.getReturnValue().thenRun(()->Minecraft.getInstance().execute(WigoClient::resourcesReloaded));
    }
}
