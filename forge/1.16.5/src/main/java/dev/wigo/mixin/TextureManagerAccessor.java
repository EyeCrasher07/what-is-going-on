package dev.wigo.mixin;

import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

@Mixin(TextureManager.class)
public interface TextureManagerAccessor {
    @Accessor("byPath") Map<ResourceLocation,Texture> wigoTextures();
}
