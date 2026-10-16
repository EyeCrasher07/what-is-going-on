package dev.wigo.protocol;

import com.google.gson.*;
import dev.wigo.platform.Platform;
import java.nio.file.Path;
import java.util.*;

public final class NetworkBatchTest {
    private static int checks;
    public static void run() {
        var states=new ArrayList<String>();for(int index=0;index<70;index++)states.add("{\"op\":\"cursor\",\"session\":7,\"x\":"+index+",\"y\":8,\"hover\":-1}");
        var frames=NetworkBatch.states(states);var decoded=new ArrayList<String>();
        for(String frame:frames){var object=new com.google.gson.JsonParser().parse(frame).getAsJsonObject();decoded.addAll(object.get("op").getAsString().equals("batch")?NetworkBatch.expandStates(object):List.of(frame));}
        check(frames.size()==3&&decoded.equals(states),"State batching changes order/count");
        states.clear();for(int index=0;index<5;index++)states.add("{\"op\":\"close\",\"title\":\""+"x".repeat(12000)+"\"}");
        for(String frame:NetworkBatch.states(states))check(frame.length()<=Wire.MAX_TEXT,"Oversized batch");
        for(String invalid:List.of("{op:batch,states:[]}","{op:batch,states:[{op:batch,states:[{op:close}]}]}","{op:batch,states:[null]}","{op:batch,states:[{op:close}],extra:0}"))reject(()->NetworkBatch.expandStates(new com.google.gson.JsonParser().parse(invalid).getAsJsonObject()));
        for(String invalid:List.of("{op:batch,activity:null,gui:[]}","{op:batch,activity:null,gui:[{op:network_reset}]}","{op:batch,activity:1,gui:[{op:ui_commit}]}"))reject(()->NetworkBatch.expandGui(new com.google.gson.JsonParser().parse(invalid).getAsJsonObject()));
        Platform.Bindings previous;try{previous=Platform.get();}catch(IllegalStateException uninitialized){previous=null;}
        var spy=new Spy();Platform.install(spy);
        String activity="{\"version\":4,\"kind\":\"gui\",\"session\":7,\"window\":0,\"x\":8,\"y\":8,\"hover\":-1,\"share\":true,\"receive\":true}";
        String update="{\"op\":\"ui_update\",\"session\":7,\"revision\":1,\"count\":0,\"title\":\"Title\",\"full\":true,\"rect\":[0,0,176,166],\"changes\":{},\"carried\":null}";
        try {
            try(var scope=NetworkBatch.client(true)){NetworkBatch.send(new Wire.Activity(activity));NetworkBatch.send(new Wire.Gui(update));check(spy.frames.isEmpty(),"Part escaped the current tick");}
            check(spy.frames.size()==1,"Cursor/update were not combined");var batch=new com.google.gson.JsonParser().parse(spy.frames.get(0)).getAsJsonObject();
            check(batch.get("activity").toString().equals(activity)&&NetworkBatch.expandGui(batch).equals(List.of(update)),"Batched activity/UI changed");
            spy.frames.clear();try(var scope=NetworkBatch.client(false)){NetworkBatch.send(new Wire.Activity(activity));NetworkBatch.send(new Wire.Gui(update));}
            check(spy.frames.equals(List.of(activity,update)),"Older connection was batched");
            spy.frames.clear();try(var outer=NetworkBatch.client(true)){NetworkBatch.send(new Wire.Activity(activity));try(var inner=NetworkBatch.client(true)){NetworkBatch.send(new Wire.Gui(update));}}
            check(spy.frames.size()==1,"Nested tick lost ordering");
            spy.frames.clear();try(var scope=NetworkBatch.client(true)){NetworkBatch.send(new Wire.Gui(update));NetworkBatch.send(Wire.Gui.legacy("{\"op\":\"network_reset\"}"));}
            check(spy.frames.equals(List.of(update,"{\"op\":\"network_reset\"}")),"Control message reordered with pending data");
        }finally{Platform.install(previous);}
        System.out.println("WIGO batching: "+checks+" checks passed; order, tick flush, old peers, control ordering, byte limits and nested rejection");
    }
    private static final class Spy implements Platform.Bindings {
        final List<String> frames=new ArrayList<>();
        public void send(net.minecraft.server.level.ServerPlayer player,Wire.State state){frames.add(state.json());}
        public void send(Wire.Activity activity){frames.add(activity.json());}public void send(Wire.Gui gui){frames.add(gui.json());}
        public boolean canSend(net.minecraft.server.level.ServerPlayer player){return true;}public boolean canSendActivity(){return true;}public boolean canSendGui(){return true;}
        public Path configDirectory(){return Path.of(".");}public boolean isModLoaded(String id){return false;}
    }
    private static void reject(Runnable action){try{action.run();throw new AssertionError("Malformed batch accepted");}catch(IllegalArgumentException|IllegalStateException expected){checks++;}}
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
