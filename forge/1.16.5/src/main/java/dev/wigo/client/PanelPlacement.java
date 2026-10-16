package dev.wigo.client;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Direction;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

/** Keep the lower edge above the floor without resizing panels in tight rooms. */
public final class PanelPlacement {
    public static double height(ClientWorld level,PlayerEntity actor,Vector3d position,double x,double z,
                                double requested,PanelShape.Vertex[] mesh,int pitch) {
        double angle=Math.toRadians(pitch),cos=Math.cos(angle),sin=Math.sin(angle),lowest=Double.POSITIVE_INFINITY;
        for(var vertex:mesh)lowest=Math.min(lowest,vertex.y()*cos-vertex.z()*sin);
        double bottom=position.y+requested+lowest,start=position.y+.25;
        if(bottom>=start)return requested;
        var hit=level.clip(new RayTraceContext(new Vector3d(x,start,z),new Vector3d(x,bottom-.025,z),
                RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,actor));
        // A wall at the starting point is a cramped room, not a floor to fit against.
        if(hit.getType()!=RayTraceResult.Type.BLOCK||hit.isInside()||hit.getDirection()!=Direction.UP)return requested;
        return Math.max(requested,hit.getLocation().y+.025-position.y-lowest);
    }
    private PanelPlacement() {}
}
