package dev.wigo.protocol;

import com.google.gson.*;
import dev.wigo.WigoServer;
import dev.wigo.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Literal fallback is requested only when the viewer lacks a local translation. */
public final class TextFallbacks {
    private record Pending(UUID actor,UUID observer,JsonElement reference,long expires){}
    private static final Map<Long,Pending> pending=new HashMap<>();
    private static long next;
    private TextFallbacks(){}
    public static boolean receive(ServerPlayer player,JsonObject packet) {
        String op=packet.has("op")?packet.get("op").getAsString():"";
        if(!op.equals("text_request")&&!op.equals("text_reply"))return false;
        long now=System.currentTimeMillis();pending.values().removeIf(value->value.expires<now);
        if(op.equals("text_request")) {
            JsonElement reference=packet.get("reference");ProtocolText.validate(reference,2048);if(!ProtocolText.isReference(reference))return true;
            ServerPlayer actor=player.level.getServer().getPlayerList().getPlayer(UUID.fromString(packet.get("actor").getAsString()));
            if(actor==null||actor.connection==null||!WigoServer.mayObserve(actor,player)||!WigoServer.hasCaption(actor,reference))return true;
            if(pending.size()>=512||pending.values().stream().filter(value->value.observer.equals(player.getUUID())).count()>=32)return true;
            long id=++next;pending.put(id,new Pending(actor.getUUID(),player.getUUID(),dev.wigo.protocol.LegacyJson.copy(reference),now+10000));
            JsonObject request=new JsonObject();request.addProperty("op","text_request");request.addProperty("request",id);request.add("reference",reference);
            Platform.get().send(actor,Wire.State.legacy(request.toString()));
        }else {
            long id=packet.get("request").getAsLong();Pending request=pending.get(id);
            if(request==null||!request.actor.equals(player.getUUID()))return true;
            JsonElement value=packet.get("value");if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString()||value.getAsString().length()>2048)return true;
            ServerPlayer observer=player.level.getServer().getPlayerList().getPlayer(request.observer);
            if(observer==null||observer.connection==null||!WigoServer.mayObserve(player,observer)||!WigoServer.hasCaption(player,request.reference)){pending.remove(id);return true;}
            pending.remove(id);JsonObject reply=new JsonObject();reply.addProperty("op","text_reply");reply.addProperty("actor",player.getUUID().toString());
            reply.add("reference",request.reference);reply.add("value",value);Platform.get().send(observer,Wire.State.legacy(reply.toString()));
        }
        return true;
    }
    public static void disconnect(UUID player){pending.values().removeIf(value->value.actor.equals(player)||value.observer.equals(player));}
    public static void clear(){pending.clear();}
}
