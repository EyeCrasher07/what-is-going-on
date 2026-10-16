package dev.wigo.config;

import java.nio.file.*;
import java.util.*;

/** Removed transport switches migrate without weakening menu/radius policy or owner comments. */
public final class MenuConfigTest {
    private static int checks;
    public static void run() {
        try {
            String generated=MenuConfig.example();var all=MenuConfig.parse(generated);
            check(all.allowsAll()&&all.maxMenuDistance()==8&&all.allowPlayerDistance(),"Default policy changed");
            check(!all.countInvisibleObservers()&&!all.countSpectatorObservers()&&all.allowObserverCounter()&&all.allowSmoothMode(),"Observer policy defaults changed");
            check(generated.startsWith("[sharing]\n")&&!generated.contains("[network]")&&!generated.contains("compact_packets")&&!generated.contains("translation_ids"),"Transport is still a configurable mode");
            check(generated.indexOf("[sharing]")<generated.indexOf("[menus]"),"Menu rules must remain last");
            for(var menu:MenuCatalog.menus())check(all.allows(menu.id())&&generated.contains("# "+menu.english()+" / "+menu.russian()+"\n\""+menu.id()+"\" = true"),"Missing rule/adjacent label: "+menu.id());
            var range=MenuConfig.parse("[sharing]\nmax_menu_distance = 16\nallow_player_distance = false\n[menus]\n\"minecraft:ender_chest\" = false\n");
            check(range.maxMenuDistance()==16&&!range.allowPlayerDistance()&&!range.allows(MenuCatalog.ENDER_CHEST),"Sharing policy ignored");
            var observerKeys=List.of("count_invisible_observers","count_spectator_observers","allow_observer_counter","allow_smooth_mode");
            for(int bits=0;bits<16;bits++) {
                StringBuilder policy=new StringBuilder("[sharing]\n");
                for(int i=0;i<4;i++)policy.append(observerKeys.get(i)).append(" = ").append((bits&(1<<i))!=0).append('\n');
                var parsed=MenuConfig.parse(policy.append("[menus]\n\"minecraft:ender_chest\" = false\n").toString());
                check(parsed.countInvisibleObservers()==((bits&1)!=0)&&parsed.countSpectatorObservers()==((bits&2)!=0)
                        &&parsed.allowObserverCounter()==((bits&4)!=0)&&parsed.allowSmoothMode()==((bits&8)!=0)
                        &&!parsed.allows(MenuCatalog.ENDER_CHEST),"Observer policy combination ignored: "+bits);
            }
            for(String key:observerKeys) {
                rejects("[sharing]\n"+key+" = 1\n[menus]\n");
                rejects("[sharing]\n"+key+" = true\n"+key+" = false\n[menus]\n");
                int at=generated.indexOf(key+" = ");
                check(at>0&&generated.substring(0,at).stripTrailing().endsWith("."),"Description must be immediately above "+key);
            }
            check(MenuCatalog.forTitle("options.videoTitle").equals("minecraft:video_settings")&&MenuCatalog.forTitle("sign.edit").equals("minecraft:sign_edit"),"Language-independent menu IDs changed");
            check(MenuCatalog.forTitle("unknown.title").equals(MenuCatalog.OTHER),"Unknown menu fallback missing");
            for(String invalid:List.of("", "[wrong]", "[menus]\n[menus]", "[menus]\n\"minecraft:chat\" = yes", "[menus]\n\"minecraft:typo\" = false", "[menus]\n\"minecraft:chat\" = true\n\"minecraft:chat\" = false", "[menus]\nminecraft:chat = false", "[sharing]\nallow_player_distance = yes\n[menus]", "[sharing]\nmax_menu_distance = 8\nmax_menu_distance = 16\n[menus]", "[sharing]\nunknown = true\n[menus]", "[network]\nunknown = true\n[menus]", "[network]\ncompact_packets = 1\n[menus]"))rejects(invalid);
            for(String value:List.of("0","129","-1","NaN","8.5","true"))rejects("[sharing]\nmax_menu_distance = "+value+"\n[menus]\n");
            Path directory=Files.createTempDirectory("wigo-native-policy-");Path file=directory.resolve(MenuConfig.FILE_NAME);
            try {
                check(MenuConfig.createIfMissing(file),"First start did not create config");
                MenuConfig.addNewMenus(file);check(generated.equals(Files.readString(file)),"Fresh config migration is not stable");
                String custom=generated.replace("count_invisible_observers = false","count_invisible_observers = true # Count invisibles")
                        .replace("count_spectator_observers = false","count_spectator_observers = true")
                        .replace("allow_observer_counter = true","allow_observer_counter = false")
                        .replace("allow_smooth_mode = true","allow_smooth_mode = false");
                Files.writeString(file,custom);MenuConfig.addNewMenus(file);var customPolicy=MenuConfig.read(file);
                check(customPolicy.countInvisibleObservers()&&customPolicy.countSpectatorObservers()
                        &&!customPolicy.allowObserverCounter()&&!customPolicy.allowSmoothMode(),"Migration changed observer choices");
                String customOnce=Files.readString(file);MenuConfig.addNewMenus(file);
                check(customOnce.equals(Files.readString(file))&&customOnce.contains("# Count invisibles"),"Observer migration repeats descriptions or loses notes");
                for(String lineEnding:List.of("\n","\r\n"))for(boolean compact:new boolean[]{false,true})for(boolean translations:new boolean[]{false,true}){
                    String legacy="# Owner notes\n[network] # My format\n# Keep this note\ncompact_packets = "+compact+" # Old choice\ntranslation_ids = "+translations+"\n[sharing]\nmax_menu_distance = 12 # My cap\nallow_player_distance = false\n[menus]\n# My private storage\n\"minecraft:ender_chest\" = false\n\"wigo:settings\" = false\n";
                    Files.writeString(file,legacy.replace("\n",lineEnding));MenuConfig.addNewMenus(file);var kept=MenuConfig.read(file);String migrated=Files.readString(file);
                    check(!migrated.contains("[network]")&&!migrated.contains("compact_packets")&&!migrated.contains("translation_ids"),"Obsolete switches survived migration");
                    check(kept.maxMenuDistance()==12&&!kept.allowPlayerDistance()&&!kept.allows(MenuCatalog.ENDER_CHEST)&&!kept.allows(MenuCatalog.SETTINGS),"Migration weakened policy");
                    for(String note:List.of("# Owner notes","# My format","# Keep this note","# Old choice","# My cap","# My private storage"))check(migrated.contains(note),"Owner comment lost: "+note);
                    MenuConfig.addNewMenus(file);check(migrated.equals(Files.readString(file)),"Repeated migration changes the file");
                }
                for(String legacy:List.of("[menus]\n\"minecraft:generic_9x3\" = false\n","[menus]\n\"minecraft:shulker_box\" = false\n")){
                    Files.writeString(file,legacy);MenuConfig.addNewMenus(file);var policy=MenuConfig.read(file);
                    check(!policy.allows(legacy.contains("generic_9x3")?MenuCatalog.ENDER_CHEST:"minecraft:shulker_box"),"Old storage prohibition exposed contents");
                    String once=Files.readString(file);MenuConfig.addNewMenus(file);check(once.equals(Files.readString(file)),"Old menu migration repeats labels");
                }
                Files.writeString(file,"[menus]\n\"minecraft:chat\" = yes\n");String invalid=Files.readString(file);
                try{MenuConfig.addNewMenus(file);throw new AssertionError("Invalid file upgraded");}catch(IllegalArgumentException expected){checks++;}
                check(invalid.equals(Files.readString(file)),"Invalid owner edit overwritten");
                check(!MenuConfig.createIfMissing(file)&&invalid.equals(Files.readString(file)),"Existing config overwritten");
            }finally{Files.deleteIfExists(file);Files.deleteIfExists(directory);}
        }catch(java.io.IOException failure){throw new AssertionError(failure);}
        System.out.println("WIGO server menu config: "+checks+" checks passed; "+MenuCatalog.menus().size()+" individual rules; automatic transport");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void rejects(String text){try{MenuConfig.parse(text);throw new AssertionError("Invalid config accepted");}catch(IllegalArgumentException expected){checks++;}}
}
