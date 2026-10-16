package dev.wigo.client;

import dev.wigo.mixin.TextureManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/** Pre-1.20 TextureManager.release deletes the GL name but retains the object. */
public final class LegacyMenuTextures {
    private LegacyMenuTextures() {}
    public static void release(ResourceLocation id) {
        var textures=((TextureManagerAccessor)Minecraft.getInstance().getTextureManager()).wigoTextures();
        var texture=textures.remove(id);
        if(texture!=null) {
            try {texture.close();} finally {texture.releaseId();}
        }
    }
}
