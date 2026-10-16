package dev.wigo.protocol;

import com.google.gson.*;
import java.util.*;

/** Byte costs include tags, definitions and flags; compare identical cold/warm workloads. */
public final class ShortTextTest {
    private static int checks;
    public static void run(){
        check(ShortTextDictionary.KEYS.size()==256,"Short dictionary size");
        var supported=new JsonObject();supported.addProperty("textFormat",ShortTextDictionary.VERSION);supported.addProperty("textDictionary",ShortTextDictionary.HASH);
        check(ShortTextDictionary.supports(supported),"New format capability rejected");
        check(!ShortTextDictionary.supports(new JsonObject()),"Older format cannot receive short IDs");
        for(var invalid:List.of(JsonNull.INSTANCE,new JsonPrimitive("1"),new JsonPrimitive(0),new JsonPrimitive(2),new JsonPrimitive(1.5),new JsonArray(),new JsonObject())){
            var next=dev.wigo.protocol.LegacyJson.copy(supported);next.add("textFormat",invalid);check(!ShortTextDictionary.supports(next),"Invalid short format capability accepted");
        }
        for(var invalid:List.of(JsonNull.INSTANCE,new JsonPrimitive("different dictionary"),new JsonPrimitive(1),new JsonArray(),new JsonObject())){
            var next=dev.wigo.protocol.LegacyJson.copy(supported);next.add("textDictionary",invalid);check(!ShortTextDictionary.supports(next),"Invalid short dictionary capability accepted");
        }
        for(int alias=0;alias<256;alias++)check(ShortTextDictionary.alias(ShortTextDictionary.key(alias))==alias,"Alias mapping");
        var encoded=new CompactCodec.Cache();var decoded=new CompactCodec.Cache();
        for(int key=0;key<WireDictionary.KEYS.size();key++){
            JsonArray args=new JsonArray();if(key%11==0){args.add("42");args.add("Привет 👋");}
            var reference=ProtocolText.reference(key,key%WireDictionary.LANGUAGES.size(),args,key%2==0);
            check(CompactCodec.decode(CompactCodec.encode(reference,encoded,true),decoded,4096).equals(reference),"Translation did not roundtrip: "+key);
        }
        for(int language=0;language<WireDictionary.LANGUAGES.size();language++)
            for(int alias:new int[]{0,127,128,255}){
                var ref=ProtocolText.reference(ShortTextDictionary.key(alias),language,new JsonArray(),false);
                check(CompactCodec.decode(CompactCodec.encode(ref,encoded,true),decoded,4096).equals(ref),"Unsigned alias/language");
            }
        var oldEnc=new CompactCodec.Cache();var newEnc=new CompactCodec.Cache();var oldDec=new CompactCodec.Cache();var newDec=new CompactCodec.Cache();
        long oldCold=0,newCold=0,oldWarm=0,newWarm=0;
        List<JsonElement> corpus=new ArrayList<>();
        for(int alias=0;alias<256;alias++)corpus.add(ProtocolText.reference(ShortTextDictionary.key(alias),WireDictionary.language("ru_ru"),new JsonArray(),false));
        for(int pass=0;pass<2;pass++)for(var value:corpus){
            byte[] old=CompactCodec.encode(value,oldEnc),now=CompactCodec.encode(value,newEnc,true);
            check(CompactCodec.decode(old,oldDec,4096).equals(value)&&CompactCodec.decode(now,newDec,4096).equals(value),"Compared workload changed text");
            if(pass==0){oldCold+=old.length;newCold+=now.length;}else{oldWarm+=old.length;newWarm+=now.length;}
        }
        check(newCold<oldCold&&newWarm<=oldWarm,"Short text grows cold/warm traffic");
        var ref=ProtocolText.reference(WireDictionary.key("container.inventory"),WireDictionary.language("ru_ru"),new JsonArray(),false);
        byte[] first=CompactCodec.encode(ref,new CompactCodec.Cache(),true);
        check(first.length==6&&first[2]==23,"Short ID or one-byte first language size");
        for(byte[] hostile:new byte[][]{{22,0,0},{23,(byte)255,0,0},{23,0,0,4},{23,0,0,2,0},{23,0,0,2,17},{25,0,(byte)255,(byte)255,0},{23,0,0}})
            try{CompactCodec.decode(hostile,new CompactCodec.Cache(),4096);throw new AssertionError("Invalid short text accepted");}catch(RuntimeException expected){checks++;}
        var mixedEncoder=new CompactCodec.Cache();var mixedDecoder=new CompactCodec.Cache();
        for(int i=0;i<80;i++){
            var value=ProtocolText.reference(i%2==0?ShortTextDictionary.key(i):i,i%WireDictionary.LANGUAGES.size(),new JsonArray(),i%3==0);
            check(CompactCodec.decode(CompactCodec.encode(value,mixedEncoder,i%2==0),mixedDecoder,4096).equals(value),"Mixed old/new text decoder");
        }
        System.out.println("WIGO short text: "+checks+" checks passed; 256-caption cold bytes "+oldCold+" -> "+newCold+", warm "+oldWarm+" -> "+newWarm);
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
