package dev.wigo.client;

import dev.wigo.client.ScreenRectangle;
import net.minecraft.util.math.MathHelper;
import org.joml.*;
import net.minecraft.util.math.vector.Matrix4f;

/** Equivalent to the axis-aligned bounds added by vanilla in 1.21.4. */
public final class LegacyBounds {
    public static ScreenRectangle transform(ScreenRectangle rect,Matrix4f matrix) {
        var a=LegacyMatrices.joml(matrix).transformPosition(rect.left(),rect.top(),0,new Vector3f());
        var b=LegacyMatrices.joml(matrix).transformPosition(rect.right(),rect.bottom(),0,new Vector3f());
        return new ScreenRectangle(MathHelper.floor(a.x),MathHelper.floor(a.y),MathHelper.floor(b.x-a.x),MathHelper.floor(b.y-a.y));
    }
}
