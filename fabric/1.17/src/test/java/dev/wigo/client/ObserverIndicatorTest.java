package dev.wigo.client;

import com.google.gson.JsonParser;
import dev.wigo.config.SharingDistance;
import dev.wigo.protocol.MirrorState;

public final class ObserverIndicatorTest {
    private static int checks;
    public static void run(){
        check(ObserverBadgeLayout.at(320,180,80,90,6).numberLeft(),"Left half puts number left");
        check(!ObserverBadgeLayout.at(320,180,240,90,6).numberLeft(),"Right half puts number right");
        check(!ObserverBadgeLayout.at(320,180,0,90,6).numberLeft(),"Left edge flips number right");
        check(ObserverBadgeLayout.at(320,180,320,90,6).numberLeft(),"Right edge flips number left");
        for(int[] size:new int[][]{{320,180},{960,540},{512,300}})
            for(int digits:new int[]{6,18,42})for(double x:new double[]{-1,0,2,10,size[0]/4.,size[0]/2.,size[0]-10,size[0],size[0]+1})
                for(double y:new double[]{-1,0,size[1]/2.,size[1],size[1]+1}){
                    var r=ObserverBadgeLayout.at(size[0],size[1],x,y,digits);
                    check(r.left()>=3&&r.right()<=size[0]-3&&r.y()>=3&&r.y()+9<=size[1]-3,"Indicator clips a screen edge");
                    check(r.numberLeft()?r.numberX()+digits<r.eyeX():r.numberX()>r.eyeX()+13,"Number overlaps eye");
                }
        check(SharingDistance.effective(3,8,true)==3,"Owner cannot reduce radius");
        check(SharingDistance.effective(128,8,true)==8,"Owner exceeds server maximum");
        check(SharingDistance.effective(1,16,false)==16,"Locked radius still honors owner preference");
        check(SharingDistance.effective(0,8,true)==1,"Radius lower bound");
        for(int invalid:new int[]{0,-1,129,Integer.MAX_VALUE})try{SharingDistance.validate(invalid);throw new AssertionError("Invalid radius accepted");}catch(IllegalArgumentException expected){checks++;}
        var state=new MirrorState();
        state.accept(new com.google.gson.JsonParser().parse("{op:open,session:1,kind:gui,rows:0,size:0,name:Actor,yaw:0}").getAsJsonObject());
        check(state.distance==8,"Older open packets change default radius");
        state.accept(new com.google.gson.JsonParser().parse("{op:distance,session:1,distance:16}").getAsJsonObject());
        check(state.distance==16,"Live radius not applied");
        state.accept(new com.google.gson.JsonParser().parse("{op:distance,session:0,distance:3}").getAsJsonObject());
        check(state.distance==16,"Stale radius applied");
        try{state.accept(new com.google.gson.JsonParser().parse("{op:distance,session:1,distance:129}").getAsJsonObject());throw new AssertionError("Unbounded remote radius");}catch(IllegalArgumentException expected){checks++;}
        check(state.distance==16,"Invalid radius partially changes state");
        System.out.println("WIGO observer indicator/radius: "+checks+" checks passed");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
