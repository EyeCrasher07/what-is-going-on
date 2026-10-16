package dev.wigo.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import java.util.*;
/** Native map game data; it is described semantically, never captured from the GPU. */
public final class LegacyMapState {
    public final List<Decoration> decorations=new ArrayList<>();
    public static LegacyMapState create(MapItemSavedData data,boolean frame) {
        var state=new LegacyMapState();
        for(var value:data.getDecorations()) {
            var d=new Decoration();d.x=value.x();d.y=value.y();d.rot=value.rot();d.renderOnFrame=!frame||value.renderOnFrame();
            d.atlasSprite=Minecraft.getInstance().getMapDecorationTextures().get(value);d.name=value.name().orElse(null);state.decorations.add(d);
        }
        return state;
    }
    public static final class Decoration {
        public byte x,y,rot;public boolean renderOnFrame;public TextureAtlasSprite atlasSprite;public Component name;
    }
}
