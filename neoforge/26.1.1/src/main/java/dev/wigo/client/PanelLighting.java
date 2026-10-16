package dev.wigo.client;

/** Per-panel adaptation to local world light; independent of GUI pixels and opacity. */
public final class PanelLighting {
    private boolean initialized,automatic;
    private long updated;
    private double current;

    public static int target(int light) {return 30+Math.round(70*Math.clamp(light,0,15)/15f);}

    /** Vanilla's daylight/weather reduction, for clients whose cached sky darkness does not tick. */
    public static int skyDarken(double time,float rain,float thunder) {
        double sun=0.5+2*Math.clamp(Math.cos(time*2*Math.PI),-0.25,0.25);
        return (int)((1-sun*(1-rain*5/16.0)*(1-thunder*5/16.0))*11);
    }

    public int update(boolean automatic,int manual,int light,long now) {
        int target=automatic?target(light):Math.clamp(manual,0,200);
        if(!initialized||!automatic||!this.automatic)current=target;
        else {
            double elapsed=Math.clamp((now-updated)/1_000_000_000.0,0,1);
            current+=(target-current)*(1-Math.exp(-elapsed/0.35));
        }
        initialized=true;this.automatic=automatic;updated=now;
        return (int)Math.round(current);
    }
}
