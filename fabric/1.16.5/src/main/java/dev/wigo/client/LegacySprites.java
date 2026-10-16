package dev.wigo.client;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Consumer;
public final class LegacySprites {
    public static void draw(GuiGraphics g,Consumer<MultiBufferSource> operation) {operation.accept(g.bufferSource());g.flush();}
    public static void blit(GuiGraphics g,ResourceLocation id,int x0,int x1,int y0,int y1,float u0,float u1,float v0,float v1,int color) {g.texture(id,x0,x1,y0,y1,u0,u1,v0,v1,color);}
    public static void atlas(GuiGraphics g,TextureAtlasSprite sprite,int x0,int y0,int x1,int y1,float u0,float u1,float v0,float v1,int color) {
        blit(g,sprite.atlas().location(),x0,x1,y0,y1,sprite.getU0()+(sprite.getU1()-sprite.getU0())*u0,sprite.getU0()+(sprite.getU1()-sprite.getU0())*u1,sprite.getV0()+(sprite.getV1()-sprite.getV0())*v0,sprite.getV0()+(sprite.getV1()-sprite.getV0())*v1,color);
    }
}
