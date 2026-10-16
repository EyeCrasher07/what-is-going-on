package dev.wigo.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
public interface OffscreenGui {
    void wigoTarget(RenderTarget target, int scale);
}
