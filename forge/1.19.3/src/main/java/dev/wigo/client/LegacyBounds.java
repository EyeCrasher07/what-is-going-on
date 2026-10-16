package dev.wigo.client;

import dev.wigo.client.ScreenRectangle;
import net.minecraft.util.Mth;
import org.joml.*;

/** Equivalent to the axis-aligned bounds added by vanilla in 1.21.4. */
public final class LegacyBounds {
    public static ScreenRectangle transform(ScreenRectangle rect,Matrix4f matrix) {
        var a=matrix.transformPosition(rect.left(),rect.top(),0,new Vector3f());
        var b=matrix.transformPosition(rect.right(),rect.bottom(),0,new Vector3f());
        return new ScreenRectangle(Mth.floor(a.x),Mth.floor(a.y),Mth.floor(b.x-a.x),Mth.floor(b.y-a.y));
    }
}
