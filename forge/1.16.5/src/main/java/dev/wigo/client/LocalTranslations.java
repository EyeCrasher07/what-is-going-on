package dev.wigo.client;

import com.google.gson.*;
import dev.wigo.protocol.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.BidiReorderer;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.ResourceLocation;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Local game resources supply captions; the mod embeds only their versioned keys. */
public final class LocalTranslations implements ProtocolText.Provider {
    @com.github.bsideup.jabel.Desugar
    private record Template(int key,String format,Pattern pattern,List<Integer> arguments,String prefix){}
    @com.github.bsideup.jabel.Desugar
    private record Data(Map<String,String> values,Map<String,Integer> logical,Map<String,Integer> visual,List<Template> templates,boolean rtl){}
    private final Map<String,Data> loaded=new LinkedHashMap<>(16,.75f,true);
    private final LinkedHashMap<String,String> originals=new LinkedHashMap<>();
    private final LinkedHashMap<String,JsonElement> guesses=new LinkedHashMap<>();
    private Object resources;
    private static final Pattern ARGUMENT=Pattern.compile("%(?:(\\d+)\\$)?s|%%");
    private static java.util.List<net.minecraft.resources.IResource> legacyResources(net.minecraft.resources.IResourceManager manager,ResourceLocation id) {
        try{return manager.getResources(id);}catch(IOException absent){return dev.wigo.compat.Java8.list();}
    }
    private synchronized Data load(String language) {
        var mc=Minecraft.getInstance();var manager=mc.getResourceManager();
        if(resources!=manager){loaded.clear();guesses.clear();resources=manager;}
        if(loaded.containsKey(language))return loaded.get(language);
        if(loaded.size()>=16)loaded.remove(loaded.keySet().iterator().next());
        Map<String,String> values=new HashMap<>();boolean available=false;
        for(String locale:language.equals("en_us")?dev.wigo.compat.Java8.list("en_us"):dev.wigo.compat.Java8.list("en_us",language)) {
            for(String namespace:dev.wigo.compat.Java8.list("minecraft","wigo"))for(var resource:legacyResources(manager,new ResourceLocation(namespace,"lang/"+locale+".json"))) {
                try(var reader=new java.io.InputStreamReader(resource.getInputStream(),java.nio.charset.StandardCharsets.UTF_8)) {
                    var json=new com.google.gson.JsonParser().parse(reader).getAsJsonObject();
                    for(var entry:json.entrySet())if(entry.getValue().isJsonPrimitive())values.put(entry.getKey(),entry.getValue().getAsString().replaceAll("%(\\d+\\$)?[\\d.]*[df]","%$1s"));
                    if(locale.equals(language))available=true;
                }catch(IOException|RuntimeException ignored){}
            }
        }
        if(!available){loaded.put(language,null);return null;}
        
        var info=mc.getLanguageManager().getLanguage(language);boolean rtl=info!=null&&info.isBidirectional();
        Map<String,Integer> logical=new HashMap<>(),visual=new HashMap<>();List<Template> templates=new ArrayList<>();
        for(int id=0;id<WireDictionary.KEYS.size();id++) {
            String text=values.get(WireDictionary.KEYS.get(id));if(text==null||text.length()>2048)continue;
            if(!text.contains("%")){logical.putIfAbsent(text,id);visual.putIfAbsent(visual(text,rtl),id);continue;}
            Matcher matcher=ARGUMENT.matcher(text);StringBuilder regex=new StringBuilder("^"),plain=new StringBuilder();
            List<Integer> args=new ArrayList<>();int end=0,automatic=0;String prefix="";
            while(matcher.find()) {
                String fixed=text.substring(end,matcher.start());regex.append(Pattern.quote(fixed));plain.append(fixed);
                if(matcher.group().equals("%%")){regex.append("%");plain.append('%');}
                else {if(args.size()==0)prefix=plain.toString();regex.append("(.{0,256}?)");args.add(matcher.group(1)==null?automatic++:Integer.parseInt(matcher.group(1))-1);}
                end=matcher.end();
            }
            regex.append(Pattern.quote(text.substring(end))).append('$');plain.append(text.substring(end));
            if(args.size()!=0&&args.size()<=16&&plain.length()>=6&&args.stream().allMatch(i->i>=0&&i<16))
                templates.add(new Template(id,text,Pattern.compile(regex.toString(),Pattern.DOTALL),dev.wigo.compat.Java8.copyList(args),prefix));
        }
        Data data=new Data(dev.wigo.compat.Java8.copyMap(values),dev.wigo.compat.Java8.copyMap(logical),dev.wigo.compat.Java8.copyMap(visual),dev.wigo.compat.Java8.copyList(templates),rtl);loaded.put(language,data);return data;
    }
    private static String visual(String text,boolean rtl) {
        StringBuilder out=new StringBuilder();BidiReorderer.reorder(ITextProperties.of(text),rtl).accept((i,style,point)->{out.appendCodePoint(point);return true;});return out.toString();
    }
    @Override public synchronized JsonElement reference(String text,boolean visual) {
        String locale=Minecraft.getInstance().getLanguageManager().getSelected().getCode();int language=WireDictionary.language(locale);
        if(language<0)return null;Data data=load(locale);if(data==null)return null;
        Integer exact=(visual?data.visual:data.logical).get(text);
        if(exact!=null)return remember(ProtocolText.reference(exact,language,new JsonArray(),visual),text);
        String cacheKey=locale+"\0"+visual+"\0"+text;
        if(guesses.containsKey(cacheKey)){var cached=guesses.get(cacheKey);return cached.isJsonNull()?null:dev.wigo.protocol.LegacyJson.copy(cached);}
        // Visual RTL arguments can move across fixed segments; ambiguous captions remain literal.
        if(visual&&data.rtl)return null;
        for(Template template:data.templates) {
            if(!text.startsWith(template.prefix))continue;Matcher match=template.pattern.matcher(text);if(!match.matches())continue;
            Map<Integer,String> args=new HashMap<>();boolean valid=true;
            for(int i=0;i<template.arguments.size();i++) {
                int index=template.arguments.get(i);String value=match.group(i+1),old=args.putIfAbsent(index,value);
                if(old!=null&&!old.equals(value)){valid=false;break;}
            }
            if(!valid)continue;int count=args.keySet().stream().max(Integer::compareTo).orElse(-1)+1;JsonArray arguments=new JsonArray();
            for(int i=0;i<count;i++)arguments.add(args.getOrDefault(i,""));
            if(!format(template.format,arguments).equals(text))continue;
            var ref=remember(ProtocolText.reference(template.key,language,arguments,visual),text);guess(cacheKey,ref);return ref;
        }
        guess(cacheKey,JsonNull.INSTANCE);return null;
    }
    private void guess(String key,JsonElement value){guesses.put(key,value);while(guesses.size()>2048)guesses.remove(guesses.keySet().iterator().next());}
    private JsonElement remember(JsonElement reference,String original) {
        originals.put(reference.toString(),original);while(originals.size()>1024)originals.remove(originals.keySet().iterator().next());return reference;
    }
    public synchronized String original(JsonElement reference){return originals.get(reference.toString());}
    public synchronized void fallback(JsonElement reference,String text){remember(reference,text);}
    public synchronized void reload(){loaded.clear();originals.clear();guesses.clear();resources=null;}
    public synchronized void reset(){loaded.clear();originals.clear();guesses.clear();resources=null;}
    @Override public synchronized String resolve(int key,int language,JsonArray arguments,boolean visual) {
        JsonElement ref=ProtocolText.reference(key,language,arguments,visual);
        Data data=load(WireDictionary.LANGUAGES.get(language));String template=data==null?null:data.values.get(WireDictionary.KEYS.get(key));
        if(template==null){String original=originals.get(ref.toString());if(original!=null)return original;throw new Missing(ref);}
        String text=format(template,arguments);if(text.length()>2048)throw new IllegalArgumentException("Expanded WIGO caption");
        return visual?visual(text,data.rtl):text;
    }
    private String format(String template,JsonArray arguments) {
        Matcher matcher=ARGUMENT.matcher(template);StringBuilder text=new StringBuilder();int end=0,automatic=0;
        while(matcher.find()) {
            text.append(template,end,matcher.start());
            if(matcher.group().equals("%%"))text.append('%');
            else {int id=matcher.group(1)==null?automatic++:Integer.parseInt(matcher.group(1))-1;if(id<0||id>=arguments.size())throw new IllegalArgumentException("Missing WIGO caption argument");var argument=arguments.get(id);if(ProtocolText.isReference(argument)){var parts=argument.getAsJsonObject().getAsJsonArray("$text");text.append(resolve(parts.get(0).getAsInt(),parts.get(1).getAsInt(),parts.get(2).getAsJsonArray(),parts.get(3).getAsBoolean()));}else text.append(argument.getAsString());}
            end=matcher.end();
        }
        return text.append(template.substring(end)).toString();
    }
    public static final class Missing extends RuntimeException {
        public final JsonElement reference;
        Missing(JsonElement reference){super("Missing local Minecraft translation");this.reference=reference;}
    }
}
