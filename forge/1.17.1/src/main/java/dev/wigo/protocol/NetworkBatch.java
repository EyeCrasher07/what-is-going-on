package dev.wigo.protocol;

import com.google.gson.*;
import dev.wigo.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import dev.wigo.protocol.LegacyPayload;
import java.util.*;

/** Collects only messages already produced in one tick; never defers them to a later tick. */
public final class NetworkBatch {
    public static final int MAX_PARTS=32;
    private static final ThreadLocal<ServerScope> SERVER=new ThreadLocal<>();
    private static final ThreadLocal<ClientScope> CLIENT=new ThreadLocal<>();
    private NetworkBatch(){}
    public static ServerScope server(){return new ServerScope();}
    public static ClientScope client(boolean enabled){return new ClientScope(enabled);}
    public static void send(ServerPlayer player,Wire.State state) {
        var scope=SERVER.get();var peer=NetworkSessions.peer(player);
        if(scope==null||peer==null||!peer.options.compact()||!peer.packetTemplates){Platform.get().send(player,state);return;}
        scope.queued.computeIfAbsent(player,ignored->new ArrayList<>()).add(state.json());
    }
    public static void send(Wire.Activity activity) {
        var scope=CLIENT.get();
        if(scope==null||!scope.enabled){Platform.get().send(activity);return;}
        if(scope.activity!=null)scope.flush();scope.activity=activity.json();
    }
    public static void send(Wire.Gui gui) {
        var scope=CLIENT.get();
        if(scope==null||!scope.enabled){Platform.get().send(gui);return;}
        var object=new com.google.gson.JsonParser().parse(gui.json()).getAsJsonObject();
        String op=object.has("op")?object.get("op").getAsString():"";
        if(!Set.of("ui_begin","ui_patch","ui_commit","ui_update","drag").contains(op)){scope.flush();Platform.get().send(gui);return;}
        if(scope.gui.size()==4)scope.flush();scope.gui.add(gui.json());
    }
    public static List<String> states(List<String> states) {
        var result=new ArrayList<String>();var parts=new ArrayList<String>();int chars=0;
        for(String state:states) {
            if(!parts.isEmpty()&&(chars+state.length()+64>Wire.MAX_TEXT||parts.size()==MAX_PARTS)){stateGroup(result,parts);parts.clear();chars=0;}
            parts.add(state);chars+=state.length()+1;
        }
        if(!parts.isEmpty())stateGroup(result,parts);return result;
    }
    private static void stateGroup(List<String> output,List<String> parts) {
        if(parts.size()==1){output.add(parts.get(0));return;}
        var batch=new JsonObject();batch.addProperty("op","batch");var states=new JsonArray();parts.forEach(part->states.add(new com.google.gson.JsonParser().parse(part)));batch.add("states",states);
        String text=batch.toString();if(text.length()>Wire.MAX_TEXT)throw new IllegalArgumentException("WIGO state batch budget");output.add(text);
    }
    public static List<String> expandStates(JsonObject batch) {
        if(batch.size()!=2||!batch.has("states"))throw new IllegalArgumentException("WIGO batch shape");
        var result=parts(batch.getAsJsonArray("states"),MAX_PARTS);
        for(String text:result)if(new com.google.gson.JsonParser().parse(text).getAsJsonObject().get("op").getAsString().equals("batch"))throw new IllegalArgumentException("Nested WIGO batch");
        return result;
    }
    public static List<String> expandGui(JsonObject batch) {
        if(batch.size()!=3||!batch.has("activity")||!batch.has("gui"))throw new IllegalArgumentException("WIGO client batch shape");
        var result=parts(batch.getAsJsonArray("gui"),4);
        for(String text:result) {
            var object=new com.google.gson.JsonParser().parse(text).getAsJsonObject();
            if(!Set.of("ui_begin","ui_patch","ui_commit","ui_update","drag").contains(object.get("op").getAsString()))throw new IllegalArgumentException("WIGO client batch operation");
        }
        var activity=batch.get("activity");
        if(!activity.isJsonNull()&&(!activity.isJsonObject()||activity.toString().length()>1024))throw new IllegalArgumentException("WIGO batched activity");
        return result;
    }
    private static List<String> parts(JsonArray values,int limit) {
        if(values.size()>limit||values.size()==0)throw new IllegalArgumentException("WIGO batch count");
        var result=new ArrayList<String>();
        for(var value:values){if(!value.isJsonObject()||value.toString().length()>Wire.MAX_TEXT)throw new IllegalArgumentException("WIGO batch part");result.add(value.toString());}return result;
    }
    public static List<LegacyPayload> uploads(String activity,List<String> gui) {
        var output=new ArrayList<LegacyPayload>();var parts=new ArrayList<String>();String held=activity;
        int chars=held==null?64:held.length()+64;
        for(String text:gui) {
            if(!parts.isEmpty()&&(chars+text.length()+1>Wire.MAX_TEXT||parts.size()==4)){uploadGroup(output,held,parts);held=null;parts.clear();chars=64;}
            if(parts.isEmpty()&&chars+text.length()+1>Wire.MAX_TEXT&&held!=null){output.add(new Wire.Activity(held));held=null;chars=64;}
            parts.add(text);chars+=text.length()+1;
        }
        uploadGroup(output,held,parts);return output;
    }
    private static void uploadGroup(List<LegacyPayload> output,String activity,List<String> parts) {
        if(parts.isEmpty()){if(activity!=null)output.add(new Wire.Activity(activity));return;}
        if(activity==null&&parts.size()==1){output.add(new Wire.Gui(parts.get(0)));return;}
        var batch=new JsonObject();batch.addProperty("op","batch");batch.add("activity",activity==null?JsonNull.INSTANCE:new com.google.gson.JsonParser().parse(activity));
        var messages=new JsonArray();parts.forEach(part->messages.add(new com.google.gson.JsonParser().parse(part)));batch.add("gui",messages);
        String text=batch.toString();if(text.length()>Wire.MAX_TEXT)throw new IllegalArgumentException("WIGO client batch budget");output.add(new Wire.Gui(text));
    }
    public static final class ServerScope implements AutoCloseable {
        private final ServerScope previous;
        private final Map<ServerPlayer,List<String>> queued=new LinkedHashMap<>();
        private ServerScope(){previous=SERVER.get();SERVER.set(this);}
        @Override public void close() {
            if(previous==null)SERVER.remove();else SERVER.set(previous);
            if(previous!=null){queued.forEach((player,parts)->previous.queued.computeIfAbsent(player,ignored->new ArrayList<>()).addAll(parts));return;}
            queued.forEach((player,parts)->{for(String text:states(parts))Platform.get().send(player,new Wire.State(text));});
        }
    }
    public static final class ClientScope implements AutoCloseable {
        private final ClientScope previous;private final boolean enabled;
        private String activity;private final List<String> gui=new ArrayList<>();
        private ClientScope(boolean enabled){this.enabled=enabled;previous=CLIENT.get();CLIENT.set(this);}
        private void flush() {
            for(var payload:uploads(activity,gui))if(payload instanceof Wire.Activity value)Platform.get().send(value);else Platform.get().send((Wire.Gui)payload);
            gui.clear();activity=null;
        }
        @Override public void close(){
            if(previous==null){CLIENT.remove();flush();return;}
            CLIENT.set(previous);if(activity!=null)NetworkBatch.send(new Wire.Activity(activity));for(String part:gui)NetworkBatch.send(new Wire.Gui(part));
        }
    }
}
