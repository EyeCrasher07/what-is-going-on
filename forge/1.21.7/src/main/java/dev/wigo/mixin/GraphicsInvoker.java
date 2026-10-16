package dev.wigo.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphics.class)
public interface GraphicsInvoker {
    @Invoker("innerBlit") void wigoBlit(RenderPipeline pipeline, ResourceLocation texture,
            int x0, int x1, int y0, int y1, float u0, float u1, float v0, float v1, int color);
}
