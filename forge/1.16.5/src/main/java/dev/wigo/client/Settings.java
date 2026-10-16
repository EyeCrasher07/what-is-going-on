package dev.wigo.client;

import com.google.gson.*;
import dev.wigo.platform.Platform;
import dev.wigo.Wigo;
import dev.wigo.config.*;
import java.util.*;
import java.nio.file.*;
import java.io.IOException;

public final class Settings {
    public static boolean enabled = true;
    public static boolean share = true, receive = true, showUnsupportedMenus = true;
    public static int transparency = 0, brightness = 100, size = 100, curvature = 0;
    public static boolean autoBrightness = false;
    public static int panelOffset = 0, panelPitch = 0;
    public static boolean realisticMirroring = false, smoothMenus = false, reverseCurvature = false;
    public static boolean showObservers = false;
    public static CursorStyle cursorStyle = CursorStyle.ARROW;
    public static int sharingDistance = SharingDistance.DEFAULT;
    public static double observerX = 0.95, observerY = 0.06;
    private static SharingRules rules = SharingRules.defaults();
    private static List<SharingRules.PlayerRef> whitelist = new ArrayList<>(), blacklist = new ArrayList<>();
    private static long rulesRevision = 1;
    public static SharingRules rules() {return rules;}
    public static boolean sharing() {return enabled && share;}
    public static boolean receiving() {return enabled && receive;}
    public static long rulesRevision() {return rulesRevision;}
    public static boolean menuEnabled(String id) {return rules.allowsMenu(id);}
    public static List<SharingRules.PlayerRef> players(SharingRules.Mode mode) {
        return dev.wigo.compat.Java8.copyList(mode == SharingRules.Mode.WHITELIST ? whitelist : blacklist);
    }
    public static void toggleMenu(String id) {
        if(!dev.wigo.config.MenuCatalog.known(id))return;
        Set<String> disabled = new HashSet<>(rules.disabled());
        if (!disabled.remove(id)) disabled.add(id);
        replaceRules(rules.mode(), rules.side(), disabled);
    }
    public static void mode(SharingRules.Mode mode) {replaceRules(mode, rules.side(), rules.disabled());}
    public static void side(SharingRules.Side side) {replaceRules(rules.mode(), side, rules.disabled());}
    private static void replaceRules(SharingRules.Mode mode, SharingRules.Side side, Collection<String> disabled) {
        rules = new SharingRules(mode, side, disabled, mode == SharingRules.Mode.ALL ? dev.wigo.compat.Java8.list() : players(mode));
        rulesRevision++; save();
    }
    public static boolean addPlayer(SharingRules.Mode mode, SharingRules.PlayerRef player) {
        if (mode == SharingRules.Mode.ALL) return false;
        List<SharingRules.PlayerRef> list = mode == SharingRules.Mode.WHITELIST ? whitelist : blacklist;
        if (list.size() >= SharingRules.MAX_PLAYERS || list.stream().anyMatch(p -> p.name().equalsIgnoreCase(player.name())
                || p.uuid() != null && p.uuid().equals(player.uuid()))) return false;
        list.add(player); replaceRules(rules.mode(), rules.side(), rules.disabled()); return true;
    }
    public static void removePlayer(SharingRules.Mode mode, SharingRules.PlayerRef player) {
        (mode == SharingRules.Mode.WHITELIST ? whitelist : blacklist).remove(player);
        replaceRules(rules.mode(), rules.side(), rules.disabled());
    }
    public static void resetMyMenus() {
        resetKeys("share","sharing","whitelist","blacklist","showObservers","sharingDistance","observerX","observerY","cursorStyle");
    }
    public static void resetOtherMenus() {
        resetKeys("receive","showUnsupportedMenus","transparency","brightness","size","curvature","autoBrightness",
                "panelOffset","panelPitch","realisticMirroring","smoothMenus","reverseCurvature");
    }
    private static void resetKeys(String... keys) {
        JsonObject settings=json();
        for(String key:keys)settings.remove(key);
        read(settings);save();
    }
    private static Path path() { return Platform.get().configDirectory().resolve("wigo.json"); }
    public static void load() {
        if (!Files.isRegularFile(path())) return;
        try {
            read(new com.google.gson.JsonParser().parse(dev.wigo.compat.Java8.read(path())).getAsJsonObject());
        } catch (IOException | RuntimeException e) { Wigo.LOG.warn("Cannot read WIGO settings; using defaults"); }
    }
    public static void save() {
        JsonObject json = json();
        try { Files.createDirectories(path().getParent()); dev.wigo.compat.Java8.write(path(), json.toString() + "\n"); }
        catch (IOException e) { Wigo.LOG.warn("Cannot save WIGO settings: {}", e.toString()); }
    }
    public static JsonObject json() {
        JsonObject json = new JsonObject(); json.addProperty("enabled",enabled);json.addProperty("share", share); json.addProperty("receive", receive);
        json.addProperty("showUnsupportedMenus", showUnsupportedMenus);
        json.addProperty("transparency", transparency); json.addProperty("size", size); json.addProperty("curvature", curvature);
        json.addProperty("brightness", brightness); json.addProperty("reverseCurvature", reverseCurvature);
        json.addProperty("autoBrightness",autoBrightness);json.addProperty("panelOffset",panelOffset);json.addProperty("panelPitch",panelPitch);
        json.addProperty("realisticMirroring", realisticMirroring); json.add("sharing", rules.json());
        json.addProperty("smoothMenus",smoothMenus);
        json.addProperty("showObservers",showObservers);json.addProperty("sharingDistance",sharingDistance);
        json.addProperty("cursorStyle",cursorStyle.key);
        json.addProperty("observerX",observerX);json.addProperty("observerY",observerY);
        JsonArray white = new JsonArray(), black = new JsonArray();
        whitelist.forEach(p -> white.add(p.json())); blacklist.forEach(p -> black.add(p.json()));
        json.add("whitelist", white); json.add("blacklist", black); return json;
    }
    /** Validate first, then apply: broken edits cannot partially weaken privacy settings. */
    public static void read(JsonObject json) {
        SharingRules next = json.has("sharing") ? SharingRules.parse(json.getAsJsonObject("sharing")) : SharingRules.defaults();
        List<SharingRules.PlayerRef> white = json.has("whitelist") ? SharingRules.parsePlayers(json.getAsJsonArray("whitelist")) : dev.wigo.compat.Java8.list();
        List<SharingRules.PlayerRef> black = json.has("blacklist") ? SharingRules.parsePlayers(json.getAsJsonArray("blacklist")) : dev.wigo.compat.Java8.list();
        boolean nextEnabled=flag(json,"enabled",true);
        boolean nextShare = flag(json,"share",true), nextReceive = flag(json,"receive",true), nextUnsupported = flag(json,"showUnsupportedMenus",true);
        boolean mirror = flag(json,"realisticMirroring",false), smooth = flag(json,"smoothMenus",false), reverse = flag(json,"reverseCurvature",false);
        int alpha = amount(json,"transparency",0,0,100), scale = amount(json,"size",100,50,200), bend = amount(json,"curvature",0,0,100);
        int light = amount(json,"brightness",100,0,200);
        boolean automatic=flag(json,"autoBrightness",false);
        int offset=amount(json,"panelOffset",0,-200,200),pitch=amount(json,"panelPitch",0,-60,60);
        boolean observers=flag(json,"showObservers",false);
        CursorStyle cursor=json.has("cursorStyle")?CursorStyle.fromKey(json.get("cursorStyle").getAsString()):CursorStyle.ARROW;
        int distance=amount(json,"sharingDistance",SharingDistance.DEFAULT,SharingDistance.MIN,SharingDistance.MAX);
        double badgeX=position(json,"observerX",0.95),badgeY=position(json,"observerY",0.06);
        rules = new SharingRules(next.mode(), next.side(), next.disabled(), next.mode() == SharingRules.Mode.ALL ? dev.wigo.compat.Java8.list()
                : next.mode() == SharingRules.Mode.WHITELIST ? white : black);
        whitelist = new ArrayList<>(white); blacklist = new ArrayList<>(black); rulesRevision++;
        enabled=nextEnabled;share = nextShare; receive = nextReceive; showUnsupportedMenus = nextUnsupported;
        transparency = alpha; size = scale; curvature = bend; realisticMirroring = mirror; smoothMenus = smooth;
        brightness = light; reverseCurvature = reverse;
        autoBrightness=automatic;panelOffset=offset;panelPitch=pitch;
        showObservers=observers;sharingDistance=distance;observerX=badgeX;observerY=badgeY;
        cursorStyle=cursor;
    }
    private static boolean flag(JsonObject json,String key,boolean fallback) {return json.has(key)?json.get(key).getAsBoolean():fallback;}
    private static int amount(JsonObject json,String key,int fallback,int min,int max) {return json.has(key)?dev.wigo.protocol.LegacyClamp.clamp(json.get(key).getAsInt(),min,max):fallback;}
    private static double position(JsonObject json,String key,double fallback) {
        double value=json.has(key)?json.get(key).getAsDouble():fallback;
        if(!Double.isFinite(value))throw new IllegalArgumentException("Invalid indicator position");
        return dev.wigo.protocol.LegacyClamp.clamp(value,0,1);
    }
}
