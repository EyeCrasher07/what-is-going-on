package dev.wigo.mixin;
import dev.wigo.client.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.world.storage.MapData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MapItemRenderer.class)
public abstract class MapCaptureMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void map(MatrixStack pose,IRenderTypeBuffer buffers,MapData data,boolean frame,int light,CallbackInfo ci) {
        if(GuiCapture.recording()) {
            GuiCapture.record(()->{var state=LegacyMapState.create(data,frame);GuiCapture.mapPrepared(state,data);GuiCapture.map(GuiCapture.pose(pose.last().pose()),state);});ci.cancel();
        }
    }
}
