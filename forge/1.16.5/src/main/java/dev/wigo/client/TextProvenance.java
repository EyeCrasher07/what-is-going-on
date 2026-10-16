package dev.wigo.client;

import com.google.gson.*;
import dev.wigo.protocol.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.IReorderingProcessor;
import java.util.*;

/** Weak source-component provenance; no sender pixels, fonts or translation files. */
public final class TextProvenance {
    private static final Map<IReorderingProcessor,JsonElement> sources=Collections.synchronizedMap(new WeakHashMap<>());
    private TextProvenance(){}
    public static void remember(ITextProperties source,IReorderingProcessor sequence){
        var mc=Minecraft.getInstance();if(mc==null||mc.getConnection()==null||!Settings.sharing()||!WigoClient.richText())return;
        if(source instanceof ITextComponent component){var ref=reference(component,true,0);if(ref==null&&component.getSiblings().isEmpty()&&!(component instanceof TranslationTextComponent))ref=JsonNull.INSTANCE;if(ref!=null){if(sources.size()>=4096)sources.clear();sources.put(sequence,ref);}}
    }
    public static void clear(){sources.clear();}
    public static JsonElement reference(IReorderingProcessor sequence){var ref=sources.get(sequence);return ref==null||ref.isJsonNull()?null:dev.wigo.protocol.LegacyJson.copy(ref);}
    public static boolean literal(IReorderingProcessor sequence){var ref=sources.get(sequence);return ref!=null&&ref.isJsonNull();}
    private static JsonElement reference(ITextComponent component,boolean visual,int depth){
        if(depth>8||!component.getSiblings().isEmpty()||!(component instanceof TranslationTextComponent translated))return null;
        int key=WireDictionary.key(translated.getKey()),language=WireDictionary.language(Minecraft.getInstance().getLanguageManager().getSelected().getCode());
        if(key<0||language<0||translated.getArgs().length>16)return null;
        JsonArray args=new JsonArray();
        for(Object arg:translated.getArgs()){
            if(arg instanceof ITextComponent child){var ref=reference(child,false,depth+1);args.add(ref==null?new JsonPrimitive(child.getString()):ref);}
            else args.add(String.valueOf(arg));
        }
        try{return ProtocolText.reference(key,language,args,visual);}catch(IllegalArgumentException invalid){return null;}
    }
}
