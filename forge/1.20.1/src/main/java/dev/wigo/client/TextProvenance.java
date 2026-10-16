package dev.wigo.client;

import com.google.gson.*;
import dev.wigo.protocol.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

/** Weak source-component provenance; no sender pixels, fonts or translation files. */
public final class TextProvenance {
    private static final Map<FormattedCharSequence,JsonElement> sources=Collections.synchronizedMap(new WeakHashMap<>());
    private TextProvenance(){}
    public static void remember(FormattedText source,FormattedCharSequence sequence){
        var mc=Minecraft.getInstance();if(mc==null||mc.getConnection()==null||!Settings.sharing()||!WigoClient.richText())return;
        if(source instanceof Component component){var ref=reference(component,true,0);if(ref==null&&component.getSiblings().isEmpty()&&!(component.getContents() instanceof TranslatableContents))ref=JsonNull.INSTANCE;if(ref!=null){if(sources.size()>=4096)sources.clear();sources.put(sequence,ref);}}
    }
    public static void clear(){sources.clear();}
    public static JsonElement reference(FormattedCharSequence sequence){var ref=sources.get(sequence);return ref==null||ref.isJsonNull()?null:ref.deepCopy();}
    public static boolean literal(FormattedCharSequence sequence){var ref=sources.get(sequence);return ref!=null&&ref.isJsonNull();}
    private static JsonElement reference(Component component,boolean visual,int depth){
        if(depth>8||!component.getSiblings().isEmpty()||!(component.getContents() instanceof TranslatableContents translated))return null;
        int key=WireDictionary.key(translated.getKey()),language=WireDictionary.language(Minecraft.getInstance().getLanguageManager().getSelected());
        if(key<0||language<0||translated.getArgs().length>16)return null;
        JsonArray args=new JsonArray();
        for(Object arg:translated.getArgs()){
            if(arg instanceof Component child){var ref=reference(child,false,depth+1);args.add(ref==null?new JsonPrimitive(child.getString()):ref);}
            else args.add(String.valueOf(arg));
        }
        try{return ProtocolText.reference(key,language,args,visual);}catch(IllegalArgumentException invalid){return null;}
    }
}
