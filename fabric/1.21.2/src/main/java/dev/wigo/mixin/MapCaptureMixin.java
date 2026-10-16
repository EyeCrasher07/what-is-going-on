package dev.wigo.mixin;
import dev.wigo.client.GuiCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.world.level.saveddata.maps.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MapRenderer.class)
public abstract class MapCaptureMixin {
    @Inject(method="extractRenderState",at=@At("RETURN"))
    private void palette(MapId id,MapItemSavedData data,MapRenderState state,CallbackInfo ci) {GuiCapture.mapPrepared(state,data);}
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void wigoPalette(MapRenderState state,PoseStack pose,MultiBufferSource buffers,boolean frame,int light,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->GuiCapture.map(GuiCapture.pose(pose.last().pose()),state));ci.cancel();}
    }
}
