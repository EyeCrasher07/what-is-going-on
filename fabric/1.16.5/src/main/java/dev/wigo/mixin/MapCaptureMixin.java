package dev.wigo.mixin;
import dev.wigo.client.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.saveddata.maps.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MapRenderer.class)
public abstract class MapCaptureMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void map(PoseStack pose,MultiBufferSource buffers,MapItemSavedData data,boolean frame,int light,CallbackInfo ci) {
        if(GuiCapture.recording()) {
            GuiCapture.record(()->{var state=LegacyMapState.create(data,frame);GuiCapture.mapPrepared(state,data);GuiCapture.map(GuiCapture.pose(pose.last().pose()),state);});ci.cancel();
        }
    }
}
