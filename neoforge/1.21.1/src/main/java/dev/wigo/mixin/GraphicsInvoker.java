package dev.wigo.mixin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(GuiGraphics.class)
public interface GraphicsInvoker {
    @Invoker("innerBlit") void wigoBlit(ResourceLocation texture,int x0,int x1,int y0,int y1,int z,float u0,float u1,float v0,float v1);
    @Invoker("innerBlit") void wigoBlitColor(ResourceLocation texture,int x0,int x1,int y0,int y1,int z,float u0,float u1,float v0,float v1,float red,float green,float blue,float alpha);
}
