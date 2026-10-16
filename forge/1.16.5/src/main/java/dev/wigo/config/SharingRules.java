package dev.wigo.config;

import com.google.gson.*;
import java.util.*;

/** Owner restrictions, enforced by the server before it builds an audience. */
public final class SharingRules {
    public enum Mode { ALL, WHITELIST, BLACKLIST }
    public enum Side { BOTH, FRONT, BACK }
    @com.github.bsideup.jabel.Desugar
    public record PlayerRef(String name, UUID uuid) {
        public PlayerRef {
            if (name == null || !name.matches("[A-Za-z0-9_]{1,16}")) throw new IllegalArgumentException("Player name");
        }
        public JsonObject json() {
            JsonObject value = new JsonObject(); value.addProperty("name", name);
            if (uuid != null) value.addProperty("uuid", uuid.toString());
            return value;
        }
    }
    public static final int MAX_PLAYERS = 64;
    public static final double PANEL_FORWARD_OFFSET = 0.85;
    public static final double SIDE_LATERAL_MARGIN = 1.0;
    private final Mode mode;
    private final Side side;
    private final Set<String> disabled;
    private final List<PlayerRef> players;
    private final Set<UUID> ids;
    private final Set<String> names;

    public SharingRules(Mode mode, Side side, Collection<String> disabled, Collection<PlayerRef> players) {
        this.mode = Objects.requireNonNull(mode); this.side = Objects.requireNonNull(side);
        if (disabled.size() > MenuCatalog.menus().size() || players.size() > MAX_PLAYERS)
            throw new IllegalArgumentException("Sharing rule budget");
        for (String id : disabled) if (!MenuCatalog.known(id)) throw new IllegalArgumentException("Unknown menu " + id);
        this.disabled = dev.wigo.compat.Java8.copySet(disabled); this.players = dev.wigo.compat.Java8.copyList(players);
        Set<UUID> ids = new HashSet<>(); Set<String> names = new HashSet<>();
        for (PlayerRef player : players) {
            if (player.uuid() != null) ids.add(player.uuid());
            else names.add(player.name().toLowerCase(Locale.ROOT));
        }
        this.ids = dev.wigo.compat.Java8.copySet(ids); this.names = dev.wigo.compat.Java8.copySet(names);
    }
    public static SharingRules defaults() {
        return new SharingRules(Mode.ALL, Side.BOTH, dev.wigo.compat.Java8.set(MenuCatalog.SETTINGS), dev.wigo.compat.Java8.list());
    }
    public Mode mode() { return mode; }
    public Side side() { return side; }
    public Set<String> disabled() { return disabled; }
    public List<PlayerRef> players() { return players; }
    public boolean allowsMenu(String id) { return !disabled.contains(id); }
    public boolean allowsPlayer(UUID id, String name) {
        boolean listed = ids.contains(id) || names.contains(name.toLowerCase(Locale.ROOT));
        return mode == Mode.ALL || (mode == Mode.WHITELIST ? listed : !listed);
    }
    public boolean allowsPosition(double dx, double dz, float yaw) {
        return allowsPosition(dx, dz, yaw, PANEL_FORWARD_OFFSET);
    }
    public boolean allowsPosition(double dx, double dz, float yaw, double panelForward) {
        if (side == Side.BOTH) return true;
        double distance = Math.hypot(dx, dz);
        if (distance < 1e-6) return false;
        double radians = Math.toRadians(yaw), sin = Math.sin(radians), cos = Math.cos(radians);
        double forward = -sin * dx + cos * dz, lateral = cos * dx + sin * dz;
        double facing = side == Side.FRONT ? forward - panelForward : panelForward - forward;
        if (facing <= 1e-6) return false;
        // Reflect the rear viewpoint across the panel, then widen each lateral boundary
        // by one world block on both faces.
        double viewedForward = side == Side.FRONT ? forward : 2 * panelForward - forward;
        double outsideMargin = Math.max(0, Math.abs(lateral) - SIDE_LATERAL_MARGIN);
        return viewedForward / Math.hypot(outsideMargin, viewedForward) >= 0.5 - 1e-9;
    }
    /** Fade only inside the same widened sector; the forbidden face is always invisible. */
    public static double cameraOpacity(Side side,double dx,double dz,float yaw,double panelForward) {
        if(side==Side.BOTH)return 1;
        if(Math.hypot(dx,dz)<1e-6)return 0;
        double radians=Math.toRadians(yaw),sin=Math.sin(radians),cos=Math.cos(radians);
        double forward=-sin*dx+cos*dz,lateral=cos*dx+sin*dz;
        double facing=side==Side.FRONT?forward-panelForward:panelForward-forward;
        if(facing<=1e-6)return 0;
        double viewedForward=side==Side.FRONT?forward:2*panelForward-forward;
        double angularMargin=(Math.sqrt(3)*viewedForward+SIDE_LATERAL_MARGIN-Math.abs(lateral))/2;
        double fraction=dev.wigo.protocol.LegacyClamp.clamp(Math.min(facing,angularMargin)/0.45,0,1);
        return fraction*fraction*(3-2*fraction);
    }
    public JsonObject json() {
        JsonObject out = new JsonObject(); out.addProperty("mode", mode.name()); out.addProperty("side", side.name());
        JsonArray menus = new JsonArray(); disabled.stream().sorted().forEach(menus::add); out.add("disabled", menus);
        JsonArray entries = new JsonArray(); players.forEach(p -> entries.add(p.json())); out.add("players", entries);
        return out;
    }
    public static SharingRules parse(JsonObject json) {
        Mode mode = Mode.valueOf(json.get("mode").getAsString()); Side side = Side.valueOf(json.get("side").getAsString());
        JsonArray menus = json.getAsJsonArray("disabled"), entries = json.getAsJsonArray("players");
        if (menus.size() > MenuCatalog.menus().size() || entries.size() > MAX_PLAYERS) throw new IllegalArgumentException("Sharing rule budget");
        Set<String> disabled = new HashSet<>(); for (var menu : menus) disabled.add(menu.getAsString());
        return new SharingRules(mode, side, disabled, parsePlayers(entries));
    }
    public static List<PlayerRef> parsePlayers(JsonArray entries) {
        if (entries.size() > MAX_PLAYERS) throw new IllegalArgumentException("Player list budget");
        List<PlayerRef> players = new ArrayList<>();
        for (var entry : entries) {
            JsonObject value = entry.getAsJsonObject();
            players.add(new PlayerRef(value.get("name").getAsString(), value.has("uuid") ? UUID.fromString(value.get("uuid").getAsString()) : null));
        }
        return players;
    }
}
