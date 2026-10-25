package dev.wigo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ChatScreen;

/** Pre-1.19.3 Screen lacks renderWithTooltip; the GameRenderer call is the common entry point. */
public final class LegacyScreenDrawing {
    public static void draw(Screen screen, PoseStack pose, int x, int y, float delta) {
        draw(screen,pose,x,y,delta,()->screen.render(pose,x,y,delta));
    }
    public static void draw(Screen screen, PoseStack pose, int x, int y, float delta, Runnable ownerDraw) {
        LegacyTooltips.updateHints();ownerDraw.run();LegacyTooltips.render(screen,pose,x,y);
        GuiCapture.begin(screen, GuiGraphics.wrap(pose), x, y);
        if (GuiCapture.active()) {
            try (var restore = new LegacyCaptureScope(); var view = GuiLayout.open(screen, x, y);
                 var viewport = GuiViewport.open(screen.width, screen.height)) {
                GuiCapture.layout(view);
                float[] point = view.pointer().point(x, y);
                if(GuiCapture.isolatedPause(screen))GuiCapture.pauseContents(screen,GuiGraphics.wrap(new PoseStack()),Math.round(point[0]),Math.round(point[1]),delta);
                else screen.render(new PoseStack(), Math.round(point[0]), Math.round(point[1]), delta);
                LegacyTooltips.render(screen,new PoseStack(),Math.round(point[0]),Math.round(point[1]));
            } catch (RuntimeException invalid) {
                GuiCapture.fail();
            } finally {
                GuiCapture.end();
            }
        }
        if (screen instanceof ChatScreen chat) ChatPreview.draw(GuiGraphics.wrap(pose), chat);
        ObserverBadge.draw(GuiGraphics.wrap(pose), screen);
    }
}
