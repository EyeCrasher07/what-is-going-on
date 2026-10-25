package dev.wigo.client;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import dev.wigo.config.CursorStyle;

/** An isolated GUI scene: no world entity or network mirror is changed for the preview. */
public record SettingsPreviewState(EntityRenderState player, float yaw, float elevation,
        int transparency, int brightness, int size, int curvature, boolean reverse, boolean mirror,
        int offset, int pitch, CursorStyle cursor, int x0, int y0, int x1, int y1, float scale,
        ScreenRectangle scissorArea) implements PictureInPictureRenderState {
    @Override public ScreenRectangle bounds() {
        return PictureInPictureRenderState.getBounds(x0,y0,x1,y1,scissorArea);
    }
}
