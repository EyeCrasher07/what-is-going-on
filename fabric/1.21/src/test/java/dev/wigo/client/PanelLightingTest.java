package dev.wigo.client;

public final class PanelLightingTest {
    public static void run() {
        check(PanelLighting.target(-1)==30&&PanelLighting.target(16)==100,"Light bounds");
        check(PanelLighting.skyDarken(0,0,0)==0&&PanelLighting.skyDarken(0.5,0,0)==11,"Legacy day/night skylight reduction");
        check(PanelLighting.skyDarken(0,1,1)>PanelLighting.skyDarken(0,1,0)&&PanelLighting.skyDarken(0,1,0)>0,"Legacy weather has no effect on light");
        for(int i=0;i<15;i++)check(PanelLighting.target(i)<PanelLighting.target(i+1),"Light response is not monotonic");
        var light=new PanelLighting();
        check(light.update(false,173,0,0)==173&&light.update(false,173,15,1)==173,"Manual brightness depends on world light");
        check(light.update(true,173,15,2)==100,"Automatic mode uses stored manual brightness");
        int dimmed=light.update(true,173,0,100_000_002);
        check(dimmed>30&&dimmed<100,"Automatic light change jumps or does not respond");
        check(light.update(false,173,0,200_000_002)==173,"Manual brightness is not restored immediately");
        var a=new PanelLighting();var b=new PanelLighting();
        a.update(true,100,15,0);b.update(true,100,15,0);
        int first=a.update(true,100,0,1_000_000_000),second=0;
        for(int i=1;i<=60;i++)second=b.update(true,100,0,Math.round(i*1_000_000_000.0/60));
        check(first==second&&first>=30&&first<40,"Adaptation depends on render frame rate");
        var dark=new PanelLighting();var bright=new PanelLighting();
        check(dark.update(true,0,0,0)==30&&bright.update(true,200,15,0)==100,"Panels share adaptation state");
        check(dark.update(true,200,0,1_000_000_000)==30,"Automatic state leaks from another panel or manual value");
        System.out.println("WIGO automatic lighting passed: manual recovery, bounded local light, smooth frame-independent transitions, independent panels");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
