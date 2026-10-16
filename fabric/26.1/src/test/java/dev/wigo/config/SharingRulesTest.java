package dev.wigo.config;

import com.google.gson.*;
import java.util.*;

public final class SharingRulesTest {
    public static void run() {
        UUID friend=UUID.fromString("44444444-1234-4321-9999-123456789abc"),other=UUID.randomUUID();
        var defaultRules=SharingRules.defaults();
        check(!defaultRules.allowsMenu(MenuCatalog.SETTINGS)&&defaultRules.allowsMenu(MenuCatalog.ENDER_CHEST),"Default privacy");
        var white=new SharingRules(SharingRules.Mode.WHITELIST,SharingRules.Side.FRONT,Set.of(MenuCatalog.ENDER_CHEST),
                List.of(new SharingRules.PlayerRef("Friend",friend),new SharingRules.PlayerRef("Offline",null)));
        check(white.allowsPlayer(friend,"Renamed")&&!white.allowsPlayer(other,"Friend"),"UUID must survive rename and reject a copied name");
        check(white.allowsPlayer(other,"oFfLiNe")&&!white.allowsPlayer(other,"Stranger"),"Offline typed names");
        check(white.allowsPosition(0,3,0)&&!white.allowsPosition(0,-3,0),"Front filter");
        check(white.allowsPosition(-3,0,90)&&!white.allowsPosition(3,0,90),"Front filter follows yaw");
        var black=new SharingRules(SharingRules.Mode.BLACKLIST,SharingRules.Side.BACK,Set.of(),white.players());
        check(!black.allowsPlayer(friend,"Renamed")&&black.allowsPlayer(other,"Stranger"),"Blacklist must invert recipient match");
        check(black.allowsPosition(0,-3,0)&&!black.allowsPosition(0,3,0),"Back filter");
        check(white.allowsPosition(3,2,0) && black.allowsPosition(3,-0.3,0),
                "The same oblique view reflected across the panel must remain visible from behind");
        check(white.allowsPosition(3.6,2,0) && black.allowsPosition(3.6,-0.3,0), "Previous outside viewpoints now fit the widened range");
        check(!white.allowsPosition(4.6,2,0) && !black.allowsPosition(4.6,-0.3,0), "Both reflected viewpoints exceed the new one-block margin");
        for(float yaw:new float[]{0,45,90,180,-90,725})for(double distance:new double[]{0.01,1,8}) {
            for(double angle:new double[]{-90,-61,-60,-59,0,59,60,61,90,180}) {
                double direction=Math.toRadians(yaw+angle),dx=-Math.sin(direction)*distance,dz=Math.cos(direction)*distance;
                double radians=Math.toRadians(yaw),fx=-Math.sin(radians),fz=Math.cos(radians),forward=fx*dx+fz*dz;
                double reflection=2*(SharingRules.PANEL_FORWARD_OFFSET-forward);
                double lateral=Math.cos(radians)*dx+Math.sin(radians)*dz;
                boolean expected=Math.abs(lateral)<=Math.sqrt(3)*forward+1+1e-8 && forward>SharingRules.PANEL_FORWARD_OFFSET+1e-6;
                check(white.allowsPosition(dx,dz,yaw)==expected,"One-block-wider front sector: "+yaw+"/"+angle);
                check(black.allowsPosition(dx+reflection*fx,dz+reflection*fz,yaw)==expected,"Mirrored back sector: "+yaw+"/"+angle);
            }
        }
        check(!white.allowsPosition(2,SharingRules.PANEL_FORWARD_OFFSET,0)
                && !black.allowsPosition(2,SharingRules.PANEL_FORWARD_OFFSET,0), "Edge-on panel belongs to neither one-sided view");
        check(white.allowsPosition(0,3,0,0) && black.allowsPosition(0,-3,0,0), "Sleeping chat uses its unshifted center");
        check(!white.allowsPosition(0,0,0)&&!black.allowsPosition(0,0,0),"Overlapping players have no front/back bearing");
        for(float yaw:new float[]{0,45,90,180,-90,725})for(double forward:new double[]{-4,0,0.85,0.86,1,1.3,2,5})
            for(double lateral:new double[]{0,1,2,3.9,4.4,4.5,8}) {
                double radians=Math.toRadians(yaw),dx=-Math.sin(radians)*forward+Math.cos(radians)*lateral,dz=Math.cos(radians)*forward+Math.sin(radians)*lateral;
                double reflection=2*(SharingRules.PANEL_FORWARD_OFFSET-forward);
                double front=SharingRules.cameraOpacity(SharingRules.Side.FRONT,dx,dz,yaw,SharingRules.PANEL_FORWARD_OFFSET);
                double rear=SharingRules.cameraOpacity(SharingRules.Side.BACK,dx-Math.sin(radians)*reflection,dz+Math.cos(radians)*reflection,yaw,SharingRules.PANEL_FORWARD_OFFSET);
                check(front>=0&&front<=1&&Math.abs(front-rear)<1e-9,"Smooth front/rear fade differs at "+yaw+"/"+forward+"/"+lateral);
                if(!white.allowsPosition(dx,dz,yaw))check(front==0,"Smooth camera reveals a forbidden face/angle");
            }
        double last=0;
        for(int i=0;i<=100;i++) {
            double opacity=SharingRules.cameraOpacity(SharingRules.Side.FRONT,0,SharingRules.PANEL_FORWARD_OFFSET+i*0.0045,0,SharingRules.PANEL_FORWARD_OFFSET);
            check(opacity>=last&&opacity-last<0.02,"Camera fade is discontinuous or reverses near panel plane");last=opacity;
        }
        check(last==1&&SharingRules.cameraOpacity(SharingRules.Side.BOTH,0,0,0,0)==1,"Full smooth opacity or two-sided panel missing");
        check(SharingRules.cameraOpacity(SharingRules.Side.BACK,0,-2,0,0)==1,"Smooth sleeping chat has the wrong panel offset");
        var roundTrip=SharingRules.parse(white.json());
        check(roundTrip.json().equals(white.json())&&!roundTrip.allowsMenu(MenuCatalog.ENDER_CHEST),"Sharing rules round trip");
        List<SharingRules.PlayerRef> tooMany=new ArrayList<>();for(int i=0;i<=SharingRules.MAX_PLAYERS;i++)tooMany.add(new SharingRules.PlayerRef("User"+i,null));
        rejects(()->new SharingRules(SharingRules.Mode.WHITELIST,SharingRules.Side.BOTH,Set.of(),tooMany));
        rejects(()->new SharingRules(SharingRules.Mode.ALL,SharingRules.Side.BOTH,Set.of("unknown:menu"),List.of()));
        rejects(()->new SharingRules.PlayerRef("not a name",friend));
        JsonObject bad=white.json();bad.addProperty("side","EVERYWHERE");rejects(()->SharingRules.parse(bad));
        System.out.println("WIGO sharing rules passed: UUID/name lists, one-block-wider front and panel-reflected rear at multiple distances/yaws, edge-on and sleeping chat, defaults, round trip and hostile bounds");
    }
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
    private static void rejects(Runnable operation) {
        try {operation.run();throw new AssertionError("Invalid sharing rules accepted");}catch(IllegalArgumentException expected) {}
    }
}
