package dev.wigo.client;

import net.minecraft.client.shader.Framebuffer;
public interface OffscreenGui {
    void wigoTarget(Framebuffer target, int scale);
}
