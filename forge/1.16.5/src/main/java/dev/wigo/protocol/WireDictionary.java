package dev.wigo.protocol;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Versioned vanilla identifiers, not translations or sender resource pixels. */
public final class WireDictionary {
    private static final String COMMON = "op actor session revision kind window x y hover share receive version menu panel rules title count id components slots carried rows size name yaw width height full rect changes needed blocked type carriedCount uiRevision network_hello network_ready network_reset decode_error format translations dictionary compact legacy none chest inventory gui unsupported open close cursor patch commit update ui_begin ui_patch ui_commit ui_update ui_demand ui_resync share_rules server_rules drag r t e b s a i o k v h n m l z q c labels crafting text_request text_reply reference request value serverTime $text minecraft:default minecraft:uniform minecraft:alt minecraft:damage minecraft:enchantment_glint_override smooth side BOTH FRONT BACK";
    public static final List<String> KEYS=load("translation-keys.txt"), LANGUAGES=load("languages.txt");
    public static final List<String> VALUES;
    public static final String HASH;
    private static final Map<String,Integer> ids=new HashMap<>(),keyIds=new HashMap<>(),languageIds=new HashMap<>();
    static {
        LinkedHashSet<String> values=new LinkedHashSet<>(Arrays.asList(COMMON.split(" ")));
        values.addAll(load("resources.txt"));VALUES=dev.wigo.compat.Java8.copyList(values);
        if(VALUES.size()>65536||KEYS.size()>65536||LANGUAGES.size()>65536)throw new IllegalStateException("WIGO dictionary limit");
        for(int i=0;i<VALUES.size();i++)ids.put(VALUES.get(i),i);
        for(int i=0;i<KEYS.size();i++)keyIds.put(KEYS.get(i),i);
        for(int i=0;i<LANGUAGES.size();i++)languageIds.put(LANGUAGES.get(i),i);
        try {
            var digest=MessageDigest.getInstance("SHA-256");
            for(var list:dev.wigo.compat.Java8.list(VALUES,KEYS,LANGUAGES))for(String value:list)digest.update((value+"\n").getBytes(StandardCharsets.UTF_8));
            HASH=dev.wigo.compat.Java8.hex(digest.digest());
        }catch(Exception failure){throw new ExceptionInInitializerError(failure);}
    }
    private WireDictionary(){}
    private static List<String> load(String file) {
        try(var stream=WireDictionary.class.getResourceAsStream(file)) {
            if(stream==null)throw new IOException("Missing WIGO dictionary "+file);
            return dev.wigo.compat.Java8.lines(new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))).filter(s->!s.isEmpty()).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));
        }catch(IOException failure){throw new ExceptionInInitializerError(failure);}
    }
    public static int id(String value){return ids.getOrDefault(value,-1);}
    public static int key(String value){return keyIds.getOrDefault(value,-1);}
    public static int language(String value){return languageIds.getOrDefault(value,-1);}
}
