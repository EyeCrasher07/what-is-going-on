package dev.wigo.mixin;
import dev.wigo.client.GuiCapture;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.world.level.saveddata.maps.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MapRenderer.class)
public abstract class MapCaptureMixin {
    @Inject(method="extractRenderState",at=@At("RETURN"))
    private void palette(MapId id,MapItemSavedData data,MapRenderState state,CallbackInfo ci) {GuiCapture.mapPrepared(state,data);}
}
