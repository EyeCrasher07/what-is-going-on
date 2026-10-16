package dev.wigo.client;

import net.minecraft.world.level.material.MaterialColor;

public final class LegacyMapColors {
    private LegacyMapColors() {}
    public static int packed(int id) {
        if (id/4==0)return 0;
        MaterialColor color=MaterialColor.MATERIAL_COLORS[id/4];
        return color==null?0:color.calculateRGBColor(id&3);
    }
}
