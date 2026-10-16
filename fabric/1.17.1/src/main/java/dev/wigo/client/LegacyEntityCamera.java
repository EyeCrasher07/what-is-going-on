package dev.wigo.client;

import net.minecraft.client.Minecraft;
import com.mojang.math.Quaternion;

/** Native inventory previews temporarily replace the entity renderer's camera. */
final class LegacyEntityCamera implements AutoCloseable {
    private final net.minecraft.client.renderer.entity.EntityRenderDispatcher renderer=Minecraft.getInstance().getEntityRenderDispatcher();
    private final Quaternion orientation=renderer.cameraOrientation();
    public void close(){renderer.overrideCameraOrientation(orientation);}
}
