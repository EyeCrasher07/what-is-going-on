package dev.wigo.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.opengl.GL11;

/** Describe 1.16's direct GUI quads without submitting the second pass to OpenGL. */
public final class LegacyImmediateGui {
    public static void record(ByteBuffer source, int mode, VertexFormat format, int count) {
        if (!GuiCapture.active() || count == 0) return;
        boolean uv = format == DefaultVertexFormat.POSITION_TEX_COLOR || format == DefaultVertexFormat.POSITION_TEX;
        // Vanilla's scrollbar keeps UV fields in its vertex format while
        // disabling texturing. Those quads are colors, not the last bound atlas.
        boolean texture = uv && LegacyGlMatrices.textured();
        boolean color = format == DefaultVertexFormat.POSITION_COLOR || format == DefaultVertexFormat.POSITION_TEX_COLOR;
        if (mode != GL11.GL_QUADS || count % 4 != 0
                || !(uv || color || format == DefaultVertexFormat.POSITION)) {
            GuiCapture.fail("Immediate GUI vertex format or primitive mode");
            return;
        }
        var id = GuiGraphics.captureTexture;
        if (texture && id != null && id.getPath().equals("textures/gui/options_background.png")) return;
        int stride = format.getVertexSize();
        if ((long) count * stride > source.limit()) {
            GuiCapture.fail("Immediate GUI buffer bounds");
            return;
        }
        ByteBuffer data = source.duplicate().order(ByteOrder.nativeOrder());
        GuiGraphics graphics = GuiGraphics.wrap(new PoseStack());
        for (int first = 0; first < count; first += 4) {
            float left = Float.POSITIVE_INFINITY, right = Float.NEGATIVE_INFINITY;
            float top = Float.POSITIVE_INFINITY, bottom = Float.NEGATIVE_INFINITY;
            float u0 = 0, u1 = 0, v0 = 0, v1 = 0;
            int topColor = LegacyGlMatrices.color(), bottomColor = topColor;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = (first + vertex) * stride;
                float x = data.getFloat(offset), y = data.getFloat(offset + 4);
                if (!Float.isFinite(x) || !Float.isFinite(y)) {
                    GuiCapture.fail("Non-finite immediate GUI coordinates");
                    return;
                }
                int value = color ? rgba(data, offset + (uv ? 20 : 12)) : LegacyGlMatrices.color();
                if (x < left) { left = x; if (texture) u0 = data.getFloat(offset + 12); }
                if (x > right) { right = x; if (texture) u1 = data.getFloat(offset + 12); }
                if (y < top) { top = y; topColor = value; if (texture) v0 = data.getFloat(offset + 16); }
                if (y > bottom) { bottom = y; bottomColor = value; if (texture) v1 = data.getFloat(offset + 16); }
            }
            int x0 = Math.round(left), y0 = Math.round(top), x1 = Math.round(right), y1 = Math.round(bottom);
            if (x0 == x1 || y0 == y1) continue;
            if (texture) {
                if (id == null) { GuiCapture.fail("Immediate GUI texture missing"); return; }
                GuiCapture.texture(graphics, id, x0, y0, x1, y1, u0, u1, v0, v1, topColor);
            } else {
                // Vanilla uses OR_REVERSE for text selection. A translucent local
                // blue rectangle preserves readable text without transmitting pixels.
                if (format == DefaultVertexFormat.POSITION) topColor = bottomColor = (topColor & 0xFFFFFF) | 0x80000000;
                GuiCapture.rectangle(graphics, x0, y0, x1, y1, topColor, bottomColor);
            }
        }
    }
    private static int rgba(ByteBuffer data, int offset) {
        return ((data.get(offset + 3) & 255) << 24) | ((data.get(offset) & 255) << 16)
                | ((data.get(offset + 1) & 255) << 8) | (data.get(offset + 2) & 255);
    }
}
