package dev.wigo.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wigo.client.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Screen.class)
public abstract class ScreenCaptureMixin {
    @WrapMethod(method="renderWithTooltip")
    private void capture(PoseStack pose,int x,int y,float delta,Operation<Void> original) {
        original.call(pose,x,y,delta);Screen screen=(Screen)(Object)this;
        GuiCapture.begin(screen,GuiGraphics.wrap(pose),x,y);
        if(GuiCapture.active()) {
            try(var restore=new LegacyCaptureScope();GuiLayout view=GuiLayout.open(screen,x,y);GuiViewport viewport=GuiViewport.open(screen.width,screen.height)) {
                GuiCapture.layout(view);float[] point=view.pointer().point(x,y);
                original.call(new PoseStack(),Math.round(point[0]),Math.round(point[1]),delta);
            } catch(RuntimeException invalid) {GuiCapture.fail();}
            finally {GuiCapture.end();}
        }
        if(screen instanceof ChatScreen chat)ChatPreview.draw(GuiGraphics.wrap(pose),chat);
        ObserverBadge.draw(GuiGraphics.wrap(pose),screen);
    }
}
