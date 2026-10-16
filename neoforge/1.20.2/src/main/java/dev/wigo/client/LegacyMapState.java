package dev.wigo.client;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import java.util.*;
public final class LegacyMapState {
    public final List<Decoration> decorations=new ArrayList<>();
    public static LegacyMapState create(MapItemSavedData data,boolean frame) {
        var state=new LegacyMapState();
        for(var value:data.getDecorations()) {
            var d=new Decoration();d.x=value.x();d.y=value.y();d.rot=value.rot();d.renderOnFrame=!frame||value.renderOnFrame();
            d.icon=value.getImage();d.name=value.name();state.decorations.add(d);
        }
        return state;
    }
    public static final class Decoration {public byte x,y,rot,icon;public boolean renderOnFrame;public Component name;}
}
