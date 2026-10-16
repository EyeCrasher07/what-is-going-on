package dev.wigo.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Keep the lower edge above the floor without resizing panels in tight rooms. */
public final class PanelPlacement {
    public static double height(ClientLevel level,Player actor,Vec3 position,double x,double z,
                                double requested,PanelShape.Vertex[] mesh,int pitch) {
        double angle=Math.toRadians(pitch),cos=Math.cos(angle),sin=Math.sin(angle),lowest=Double.POSITIVE_INFINITY;
        for(var vertex:mesh)lowest=Math.min(lowest,vertex.y()*cos-vertex.z()*sin);
        double bottom=position.y+requested+lowest,start=position.y+.25;
        if(bottom>=start)return requested;
        var hit=level.clip(new ClipContext(new Vec3(x,start,z),new Vec3(x,bottom-.025,z),
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,actor));
        // A wall at the starting point is a cramped room, not a floor to fit against.
        if(hit.getType()!=HitResult.Type.BLOCK||hit.isInside()||hit.getDirection()!=Direction.UP)return requested;
        return Math.max(requested,hit.getLocation().y+.025-position.y-lowest);
    }
    private PanelPlacement() {}
}
