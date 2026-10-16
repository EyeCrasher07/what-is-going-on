package dev.wigo.config;

/** Server policy caps every owner preference, including values from older clients. */
public final class SharingDistance {
    public static final int DEFAULT=8,MIN=1,MAX=128;
    private SharingDistance(){}
    public static int validate(int value){
        if(value<MIN||value>MAX)throw new IllegalArgumentException("Menu distance must be 1–128 blocks");
        return value;
    }
    public static int effective(int requested,int maximum,boolean personal){
        validate(maximum);
        return personal?Math.clamp(requested,MIN,maximum):maximum;
    }
}
