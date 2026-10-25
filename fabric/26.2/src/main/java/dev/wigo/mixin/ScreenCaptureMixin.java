package dev.wigo.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.wigo.client.GuiCapture;
import dev.wigo.client.GuiLayout;
import dev.wigo.client.GuiViewport;
import dev.wigo.client.ChatPreview;
import dev.wigo.client.ObserverBadge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Screen.class)
public abstract class ScreenCaptureMixin {
    @WrapMethod(method="extractRenderStateWithTooltipAndSubtitles")
    private void capture(GuiGraphicsExtractor graphics, int x, int y, float delta, Operation<Void> original) {
        original.call(graphics,x,y,delta);
        Screen screen = (Screen)(Object)this;
        GuiCapture.begin(screen, graphics, x, y);
        if (GuiCapture.active()) {
            try (GuiLayout view = GuiLayout.open(screen, x, y); GuiViewport viewport = GuiViewport.open(screen.width, screen.height)) {
                GuiCapture.layout(view);
                float[] point = view.pointer().point(x, y);
                var scratch = new GuiGraphicsExtractor(Minecraft.getInstance(), new GuiRenderState(), Math.round(point[0]), Math.round(point[1]));
                if(GuiCapture.isolatedPause(screen))
                    GuiCapture.pauseContents(screen,scratch,Math.round(point[0]),Math.round(point[1]),delta);
                else original.call(scratch, Math.round(point[0]), Math.round(point[1]), delta);
            } catch (RuntimeException invalid) { GuiCapture.fail(); }
            finally { GuiCapture.end(); }
        }
        if ((Object)this instanceof ChatScreen chat) ChatPreview.draw(graphics,chat);
        ObserverBadge.draw(graphics,screen);
    }
}
