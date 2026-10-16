package dev.wigo.client;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.storage.MapData;
import java.util.*;
public final class LegacyMapState {
    public final List<Decoration> decorations=new ArrayList<>();
    public static LegacyMapState create(MapData data,boolean frame) {
        var state=new LegacyMapState();
        for(var value:data.decorations.values()) {
            var d=new Decoration();d.x=value.getX();d.y=value.getY();d.rot=value.getRot();d.renderOnFrame=!frame||value.renderOnFrame();
            d.icon=value.getImage();d.name=value.getName();state.decorations.add(d);
        }
        return state;
    }
    public static final class Decoration {public byte x,y,rot,icon;public boolean renderOnFrame;public ITextComponent name;}
}
