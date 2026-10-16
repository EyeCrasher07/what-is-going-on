package dev.wigo.protocol;

import com.google.gson.*;
import java.util.*;

/** Styled/nested references roundtrip, remain bounded and preserve user strings. */
public final class RichTextTest {
    private static int checks;
    public static void run(){
        var hello=NetworkControl.hello();check(NetworkControl.compatible(hello),"Current automatic format incompatible");
        var oldHello=dev.wigo.protocol.LegacyJson.copy(hello);oldHello.addProperty("dictionary","3c341cc7deccde28546e2106a940cff68840163b77f5cc13dab39995bf29a2b5");check(!NetworkControl.compatible(oldHello),"Old dictionary may receive new WIGO caption IDs");
        check(!NetworkControl.compatible(new JsonObject()),"Unnegotiated peer can receive compact IDs");
        var provider=ProtocolText.provider();
        try{
            ProtocolText.provider(new ProtocolText.Provider(){
                public JsonElement reference(String text,boolean visual){int key=WireDictionary.key(text.equals("Inventory")?"container.inventory":text.equals("Chest")?"container.chest":"");return key<0?null:ProtocolText.reference(key,0,new JsonArray(),visual);}
                public String resolve(int key,int language,JsonArray args,boolean visual){return WireDictionary.KEYS.get(key).equals("container.inventory")?"Inventory":"Chest";}
            });
            var command=new com.google.gson.JsonParser().parse("['t',null,null,'Inventory',100,20,-1,true,0,[['Inventory','minecraft:default',16711935,1]],[0,110,90]]").getAsJsonArray();
            var packet=new JsonObject();packet.addProperty("op","ui_patch");var changes=new JsonObject();changes.add("0",command);packet.add("changes",changes);
            var encoded=ProtocolText.encode(packet,true,true);var tree=encoded.getAsJsonObject().getAsJsonObject("changes").getAsJsonArray("0");
            check(ProtocolText.isReference(tree.get(3))&&ProtocolText.isReference(tree.get(9).getAsJsonArray().get(0).getAsJsonArray().get(0)),"Styled captions stayed literal");
            GuiScene.validateCommand(tree);
            for(boolean shortText:new boolean[]{false,true}){
                var writer=new CompactCodec.Cache();var reader=new CompactCodec.Cache();
                for(int i=0;i<25;i++)check(ProtocolText.resolve(CompactCodec.decode(CompactCodec.encode(encoded,writer,shortText),reader,16384)).equals(packet),"Styles/color/placement changed");
            }
            var old=ProtocolText.encode(packet,true,false).getAsJsonObject().getAsJsonObject("changes").getAsJsonArray("0");
            check(old.get(9).getAsJsonArray().get(0).getAsJsonArray().get(0).isJsonPrimitive(),"Compatibility mode sends rich style references");
            var typed=dev.wigo.protocol.LegacyJson.copy(packet);typed.addProperty("title","Inventory");typed.addProperty("literalTitle",true);var literals=new JsonArray();literals.add(0);typed.add("literalChanges",literals);
            check(ProtocolText.encode(typed,true).equals(typed),"Typed text/custom title matching a dictionary word is translated");
            var user=dev.wigo.protocol.LegacyJson.copy(packet);var custom=user.getAsJsonObject("changes").getAsJsonArray("0");custom.set(3,new JsonPrimitive("Склад друга 📦"));custom.get(9).getAsJsonArray().get(0).getAsJsonArray().set(0,new JsonPrimitive("Склад друга 📦"));
            check(ProtocolText.encode(user,true).equals(user),"User text changed");
            JsonArray args=new JsonArray();args.add(ProtocolText.reference(WireDictionary.key("container.chest"),0,new JsonArray(),false));
            var nested=ProtocolText.reference(WireDictionary.key("container.inventory"),0,args,true);
            check(ProtocolText.isReference(nested)&&CompactCodec.decode(CompactCodec.encode(nested,new CompactCodec.Cache(),true),new CompactCodec.Cache(),4096).equals(nested),"Nested captions lost key/language");
            for(int width:new int[]{0,-1,8193}){var bad=dev.wigo.protocol.LegacyJson.copy(command);bad.get(10).getAsJsonArray().set(2,new JsonPrimitive(width));rejects(()->GuiScene.validateCommand(bad));}
            var bad=dev.wigo.protocol.LegacyJson.copy(command);bad.get(10).getAsJsonArray().set(0,new JsonPrimitive(7));rejects(()->GuiScene.validateCommand(bad));
            var badStyle=dev.wigo.protocol.LegacyJson.copy(command);badStyle.get(9).getAsJsonArray().get(0).getAsJsonArray().set(3,new JsonPrimitive(32));rejects(()->GuiScene.validateCommand(badStyle));
            var deep=nested;for(int i=0;i<12;i++){var parts=dev.wigo.protocol.LegacyJson.copy(deep);var next=new JsonArray();next.add(deep);parts.getAsJsonArray("$text").set(2,next);deep=parts;}
            check(!ProtocolText.isReference(deep),"Unbounded nested translation accepted");
        }finally{ProtocolText.provider(provider);}
        System.out.println("WIGO rich text: "+checks+" checks passed; styled IDs, nesting, compatibility, custom text, placement and bounds");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void rejects(Runnable action){try{action.run();throw new AssertionError("Invalid rich text accepted");}catch(RuntimeException expected){checks++;}}
}
