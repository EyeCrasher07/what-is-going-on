package dev.wigo.protocol;

import com.google.gson.JsonObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Stable one-byte aliases without renumbering the existing translation dictionary. */
public final class ShortTextDictionary {
    public static final int VERSION=1;
    public static final List<String> KEYS;
    public static final String HASH;
    private static final int[] aliases=new int[WireDictionary.KEYS.size()],keys=new int[256];
    static {
        Arrays.fill(aliases,-1);
        try(var input=ShortTextDictionary.class.getResourceAsStream("short-text-keys.txt")){
            if(input==null)throw new IOException("Missing short text dictionary");
            KEYS=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8)).lines().filter(s->!s.isEmpty()).toList();
            if(KEYS.size()!=256||new HashSet<>(KEYS).size()!=256||WireDictionary.LANGUAGES.size()>256)throw new IOException("Invalid short dictionary size");
            for(int i=0;i<KEYS.size();i++){
                int key=WireDictionary.key(KEYS.get(i));if(key<0)throw new IOException("Unknown short text key");
                keys[i]=key;aliases[key]=i;
            }
            var digest=MessageDigest.getInstance("SHA-256");
            for(String key:KEYS)digest.update((key+"\n").getBytes(StandardCharsets.UTF_8));
            HASH=HexFormat.of().formatHex(digest.digest());
        }catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    private ShortTextDictionary(){}
    public static int alias(int key){return key>=0&&key<aliases.length?aliases[key]:-1;}
    public static int key(int alias){if(alias<0||alias>=256)throw new IllegalArgumentException("Short text alias");return keys[alias];}
    public static boolean supports(JsonObject hello){
        var format=hello.get("textFormat");var dictionary=hello.get("textDictionary");
        return format!=null&&format.isJsonPrimitive()&&format.getAsJsonPrimitive().isNumber()
                &&format.getAsString().equals(Integer.toString(VERSION))
                &&dictionary!=null&&dictionary.isJsonPrimitive()&&dictionary.getAsJsonPrimitive().isString()
                &&HASH.equals(dictionary.getAsString());
    }
}
