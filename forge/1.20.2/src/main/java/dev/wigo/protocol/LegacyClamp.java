package dev.wigo.protocol;
/** Java 17 equivalents of the later JDK's bounded numeric helpers. */
public final class LegacyClamp {
    private LegacyClamp() {}
    public static int clamp(int value,int min,int max) {return Math.max(min,Math.min(max,value));}
    public static float clamp(float value,float min,float max) {return Math.max(min,Math.min(max,value));}
    public static double clamp(double value,double min,double max) {return Math.max(min,Math.min(max,value));}
}
