package dev.wigo.protocol;

import com.google.gson.*;
import java.util.*;

/** Typed translation references survive a dedicated server without language assets. */
public final class ProtocolText {
    public interface Provider {
        JsonElement reference(String text,boolean visual);
        String resolve(int key,int language,JsonArray arguments,boolean visual);
    }
    private static volatile Provider provider;
    private ProtocolText(){}
    public static void provider(Provider value){provider=value;}
    public static Provider provider(){return provider;}
    public static boolean isReference(JsonElement value){return isReference(value,0);}
    private static boolean argument(JsonElement arg,int depth){return depth<=8&&(arg.isJsonPrimitive()&&arg.getAsJsonPrimitive().isString()&&arg.getAsString().length()<=2048||isReference(arg,depth));}
    private static boolean isReference(JsonElement value,int depth){
        if(depth>8)return false;
        if(!value.isJsonObject()||value.getAsJsonObject().size()!=1||!value.getAsJsonObject().has("$text"))return false;
        JsonElement parts=value.getAsJsonObject().get("$text");if(!parts.isJsonArray()||parts.getAsJsonArray().size()!=4)return false;
        var array=parts.getAsJsonArray();return array.get(0).isJsonPrimitive()&&array.get(0).getAsJsonPrimitive().isNumber()
                &&array.get(1).isJsonPrimitive()&&array.get(1).getAsJsonPrimitive().isNumber()
                &&array.get(0).toString().matches("(0|[1-9][0-9]{0,4})")&&array.get(1).toString().matches("(0|[1-9][0-9]{0,4})")
                &&array.get(0).getAsInt()<WireDictionary.KEYS.size()&&array.get(1).getAsInt()<WireDictionary.LANGUAGES.size()
                &&array.get(2).isJsonArray()&&array.get(2).getAsJsonArray().size()<=16
                &&array.get(2).getAsJsonArray().asList().stream().allMatch(arg->argument(arg,depth+1))
                &&array.get(3).isJsonPrimitive()&&array.get(3).getAsJsonPrimitive().isBoolean();
    }
    public static JsonObject reference(int key,int language,JsonArray arguments,boolean visual) {
        if(key<0||key>=WireDictionary.KEYS.size()||language<0||language>=WireDictionary.LANGUAGES.size()
                ||arguments.size()>16)throw new IllegalArgumentException("WIGO translation reference");
        for(JsonElement arg:arguments)if(!argument(arg,1))
            throw new IllegalArgumentException("WIGO translation argument");
        JsonArray parts=new JsonArray();parts.add(key);parts.add(language);parts.add(arguments.deepCopy());parts.add(visual);
        JsonObject result=new JsonObject();result.add("$text",parts);return result;
    }
    public static void validate(JsonElement value,int maxChars) {
        if(isReference(value)) {
            var object=value.getAsJsonObject();var parts=object.getAsJsonArray("$text");
            if(object.size()!=1||parts.size()!=4)throw new IllegalArgumentException("WIGO translation shape");
            reference(parts.get(0).getAsInt(),parts.get(1).getAsInt(),parts.get(2).getAsJsonArray(),parts.get(3).getAsBoolean());
            if(value.toString().length()>maxChars)throw new IllegalArgumentException("WIGO translated text length");
        }else if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString()||value.getAsString().length()>maxChars)
            throw new IllegalArgumentException("WIGO text length or type");
    }
    public static JsonElement encode(JsonElement value,boolean enabled){return encode(value,enabled,true);}
    public static JsonElement encode(JsonElement value,boolean enabled,boolean rich){
        var encoded=transform(value,enabled,false,false,false,0,rich);
        if(value.isJsonObject()){
            var source=value.getAsJsonObject();var result=encoded.getAsJsonObject();
            if(source.has("literalTitle")&&source.get("literalTitle").getAsBoolean()&&source.has("title"))result.add("title",source.get("title").deepCopy());
            if(source.has("literalChanges")&&source.has("changes")){
                var indices=source.getAsJsonArray("literalChanges");if(indices.size()>2048)throw new IllegalArgumentException("Literal caption bounds");
                for(var index:indices){String id=Integer.toString(index.getAsInt());if(source.getAsJsonObject("changes").has(id))result.getAsJsonObject("changes").add(id,source.getAsJsonObject("changes").get(id).deepCopy());}
            }
        }
        return encoded;
    }
    public static JsonElement resolve(JsonElement value){return transform(value,false,true,false,false,0,true);}
    private static JsonElement transform(JsonElement value,boolean enabled,boolean resolve,boolean visible,boolean visual,int scope,boolean rich) {
        Provider active=provider;
        if(isReference(value)&&visible) {
            validate(value,4096);
            if(!resolve||active==null)return value.deepCopy();
            var parts=value.getAsJsonObject().getAsJsonArray("$text");
            return new JsonPrimitive(active.resolve(parts.get(0).getAsInt(),parts.get(1).getAsInt(),parts.get(2).getAsJsonArray(),parts.get(3).getAsBoolean()));
        }
        if(value.isJsonObject()) {
            JsonObject next=new JsonObject();
            var object=value.getAsJsonObject();boolean gui=scope==0&&object.has("op")&&object.get("op").getAsString().startsWith("ui_");
            boolean visualTitle=scope==0&&object.has("kind")&&object.get("kind").getAsString().equals("chest");
            object.entrySet().forEach(entry->next.add(entry.getKey(),transform(entry.getValue(),enabled,resolve,scope==4||scope==0&&entry.getKey().equals("title"),scope==4||visualTitle&&entry.getKey().equals("title"),
                    gui&&entry.getKey().equals("changes")?1:scope==0&&entry.getKey().equals("labels")?4:scope==1?2:3,rich)));
            return next;
        }
        if(value.isJsonArray()) {
            var array=value.getAsJsonArray();JsonArray next=new JsonArray();
            boolean caption=scope==2&&array.size()>=9&&array.size()<=11&&array.get(0).isJsonPrimitive()&&array.get(0).getAsString().equals("t");
            for(int i=0;i<array.size();i++)next.add(transform(array.get(i),enabled,resolve,caption&&i==3||scope==6&&i==0,caption&&i==3||scope==6&&i==0,caption&&i==9&&rich?5:scope==5?6:3,rich));
            if(resolve&&caption&&next.size()>=10&&!next.get(9).isJsonNull()){
                StringBuilder plain=new StringBuilder();for(var run:next.get(9).getAsJsonArray())plain.append(run.getAsJsonArray().get(0).getAsString());next.set(3,new JsonPrimitive(plain.toString()));
            }
            return next;
        }
        if(enabled&&visible&&active!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()) {
            JsonElement ref=active.reference(value.getAsString(),visual);
            if(ref!=null&&ref.toString().length()<=(visual?2048:256))return ref;
        }
        return value.deepCopy();
    }
}
