package dev.wigo.client;

import dev.wigo.config.CursorStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Local GUI artwork; older clients use the same small icons bundled with WIGO. */
public final class CursorSprites {
    private static final Map<CursorStyle,Identifier> textures=new EnumMap<>(CursorStyle.class);
    private CursorSprites() {}
    public static void clear() {textures.clear();}
    private static Identifier texture(CursorStyle style) {
        return textures.computeIfAbsent(style,value->{
            Identifier nativeTexture=Identifier.withDefaultNamespace("textures/gui/sprites/"+value.texture);
            try {
                // ResourceManager returned a closeable resource before Optional was introduced.
                Object resource=Minecraft.getInstance().getResourceManager().getResource(nativeTexture);
                if(resource instanceof Optional<?>) {
                    if(((Optional<?>)resource).isPresent())return nativeTexture;
                } else if(resource!=null) {
                    if(resource instanceof java.io.Closeable)((java.io.Closeable)resource).close();
                    return nativeTexture;
                }
            } catch(Exception missing) { /* This sprite is absent from older Minecraft versions. */ }
            return Identifier.fromNamespaceAndPath("wigo","textures/gui/cursor/"+value.key+".png");
        });
    }
    public static void draw(GuiGraphics g,CursorStyle style,int x,int y) {
        if(style==CursorStyle.ARROW){arrow(g,x,y);return;}
        g.blit(RenderPipelines.GUI_TEXTURED,texture(style),x-style.width/2,y-style.height/2,
                0,0,style.width,style.height,style.width,style.height);
    }
    static void icon(GuiGraphics g,CursorStyle style,int x,int y) {
        if(style==CursorStyle.ARROW){arrow(g,x+3,y);return;}
        g.blit(RenderPipelines.GUI_TEXTURED,texture(style),x,y,0,0,16,16,16,16);
    }
    private static void arrow(GuiGraphics g,int x,int y) {
        String[] rows={"B", "BB", "BWB", "BWWB", "BWWWB", "BWWWWB", "BWWWWWB", "BWWWWWWB",
                "BWWWWWWWB", "BWWWWBBBBB", "BWWBWB", "BWB BWB", "BB  BWB", "B    BWB", "     BWB", "      BB"};
        for(int row=0;row<rows.length;row++)for(int col=0;col<rows[row].length();col++) {
            char c=rows[row].charAt(col);
            if(c!=' ')g.fill(x+col,y+row,x+col+1,y+row+1,c=='W'?0xFFFFFFFF:0xFF000000);
        }
    }
}
