package dev.wigo.protocol;

import com.google.gson.*;
import dev.wigo.WigoServer;
import dev.wigo.GuiRelay;
import dev.wigo.config.ServerMenus;
import dev.wigo.platform.Platform;
import net.minecraft.server.level.ServerPlayer;

/** Control messages always have legacy framing, including cache recovery. */
public final class NetworkControl {
    private NetworkControl(){}
    public static JsonObject hello(){
        JsonObject value=new JsonObject();value.addProperty("op","network_hello");value.addProperty("format",5);
        value.addProperty("dictionary",WireDictionary.HASH);value.addProperty("smooth",true);value.addProperty("observers",true);
        value.addProperty("sceneFormat",2);value.addProperty("packetFormat",1);value.addProperty("textFormat",ShortTextDictionary.VERSION);value.addProperty("textDictionary",ShortTextDictionary.HASH);return value;
    }
    public static boolean compatible(JsonObject json){
        var format=json.get("format");var dictionary=json.get("dictionary");
        return format!=null&&format.isJsonPrimitive()&&format.getAsJsonPrimitive().isNumber()&&format.getAsString().equals("5")
                &&dictionary!=null&&dictionary.isJsonPrimitive()&&dictionary.getAsJsonPrimitive().isString()&&WireDictionary.HASH.equals(dictionary.getAsString());
    }
    public static boolean receive(ServerPlayer player,JsonObject json) {
        String op=json.has("op")?json.get("op").getAsString():"";
        if(!op.equals("network_hello")&&!op.equals("network_reset")&&!op.equals("decode_error"))return false;
        NetworkSessions.Peer peer=NetworkSessions.peer(player);if(peer==null)return true;
        if(op.equals("network_hello")) {
            if(peer.negotiated)return true;
            ServerMenus.tick(player.level().getServer());
            boolean compatible=compatible(json);
            peer.smoothViewing=json.has("smooth")&&json.get("smooth").getAsBoolean();
            peer.observerCounts=json.has("observers")&&json.get("observers").getAsBoolean();
            var selected=peer.negotiate(new NetworkSessions.Options(compatible,compatible));
            peer.shortText=selected.compact()&&ShortTextDictionary.supports(json);
            peer.richText=selected.compact()&&json.has("sceneFormat")&&json.get("sceneFormat").getAsInt()==2;
            peer.packetTemplates=selected.compact()&&supportsPacketTemplates(json);
            JsonObject reply=new JsonObject();reply.addProperty("op","network_ready");reply.addProperty("format",selected.compact()?5:4);
            reply.addProperty("translations",selected.translations());reply.addProperty("dictionary",WireDictionary.HASH);
            reply.addProperty("smooth",true);
            reply.addProperty("observers",true);
            reply.addProperty("sceneFormat",peer.richText?2:1);reply.addProperty("textFormat",peer.shortText?ShortTextDictionary.VERSION:0);reply.addProperty("textDictionary",ShortTextDictionary.HASH);
            reply.addProperty("packetFormat",peer.packetTemplates?1:0);
            reply.addProperty("serverTime",System.currentTimeMillis());Platform.get().send(player,Wire.State.legacy(reply.toString()));
        }else if(op.equals("decode_error")) {
            GuiRelay.invalidateSource(player);
            Platform.get().send(player,Wire.State.legacy("{\"op\":\"network_reset\"}"));
        }else {
            peer.resetEncoder();WigoServer.resnapshot(player);
        }
        return true;
    }
    public static boolean supportsPacketTemplates(JsonObject json) {
        var value=json.get("packetFormat");
        return value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isNumber()&&value.getAsString().equals("1");
    }
}
