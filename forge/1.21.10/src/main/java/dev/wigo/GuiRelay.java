package dev.wigo;

import com.google.gson.*;
import dev.wigo.protocol.*;
import dev.wigo.config.SharingRules;
import dev.wigo.config.CursorStyle;
import dev.wigo.platform.Platform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Relays validated vanilla drawing descriptions only to the existing nearby audience. */
public final class GuiRelay {
    public static void invalidateSource(ServerPlayer actor){sources.remove(actor.getUUID());}
    public static void resnapshot(ServerPlayer observer){for(Flow flow:flows.values())flow.watchers.remove(observer.getUUID());}
    public static boolean hasCaption(UUID actor,JsonElement reference) {
        GuiScene source=sources.get(actor);if(source==null||!source.ready)return false;
        if(source.titleValue().equals(reference))return true;
        for(var command:source.commands())if(command.get(0).getAsString().equals("t")){
            if(contains(command.get(3),reference))return true;
            if(command.size()>=10&&!command.get(9).isJsonNull())for(var run:command.get(9).getAsJsonArray())if(contains(run.getAsJsonArray().get(0),reference))return true;
        }
        return false;
    }
    private static boolean contains(JsonElement value,JsonElement reference){
        if(value.equals(reference))return true;
        if(ProtocolText.isReference(value))for(var arg:value.getAsJsonObject().getAsJsonArray("$text").get(2).getAsJsonArray())if(contains(arg,reference))return true;
        return false;
    }
    private static final Map<UUID, GuiScene> sources = new HashMap<>();
    private static final Map<UUID, Flow> flows = new HashMap<>();

    public static void receive(ServerPlayer actor, JsonObject packet, long session) {
        if (packet.get("session").getAsLong() != session) return;
        GuiScene scene = sources.computeIfAbsent(actor.getUUID(), ignored -> new GuiScene(session, 0));
        if (scene.session != session) { scene = new GuiScene(session, 0); sources.put(actor.getUUID(), scene); }
        try { scene.accept(packet); }
        catch (RuntimeException invalid) {
            sources.remove(actor.getUUID());
            JsonObject resync = new JsonObject(); resync.addProperty("op", "ui_resync"); resync.addProperty("session", session);
            send(resync, List.of(actor));
            Wigo.LOG.debug("Rejected WIGO GUI description: {}", invalid.toString());
        }
    }
    public static void tick(ServerPlayer actor, long session, String kind, String panel, float yaw, List<ServerPlayer> watchers, float x, float y,SharingRules.Side side,int distance) {
        tick(actor,session,kind,panel,yaw,watchers,x,y,side,distance,CursorStyle.ARROW);
    }
    public static void tick(ServerPlayer actor, long session, String kind, String panel, float yaw, List<ServerPlayer> watchers, float x, float y,SharingRules.Side side,int distance,CursorStyle cursorStyle) {
        UUID owner = actor.getUUID();
        Flow flow = flows.get(owner);
        GuiScene source = sources.get(owner);
        if (source != null && source.session != session) { sources.remove(owner); source = null; }
        if (flow != null && (flow.session != session || !flow.kind.equals(kind) || !flow.panel.equals(panel) || kind.equals("gui") && source != null && source != flow.source)) {
            close(actor.level().getServer(), flow); flows.remove(owner); flow = null;
        }
        if (kind.equals("gui") && (source == null || !source.ready)) return;
        if (flow == null) { flow = new Flow(actor, session, kind, panel, yaw, source); flows.put(owner, flow); }
        Set<UUID> next = new HashSet<>();
        for (ServerPlayer observer : watchers) next.add(observer.getUUID());
        for (UUID id : flow.watchers) if (!next.contains(id)) closeOne(actor.level().getServer(), flow, id);
        List<ServerPlayer> newcomers = new ArrayList<>(), existing = new ArrayList<>();
        for (ServerPlayer observer : watchers) (flow.watchers.contains(observer.getUUID()) ? existing : newcomers).add(observer);
        if(flow.distance!=distance) {
            flow.distance=distance;JsonObject changed=base(flow,"distance");changed.addProperty("distance",distance);send(changed,existing);
        }
        if(flow.side!=side){
            flow.side=side;JsonObject changedSide=base(flow,"side");changedSide.addProperty("side",side.name());
            send(changedSide,existing.stream().filter(p->{var peer=NetworkSessions.peer(p);return peer!=null&&peer.smoothViewing;}).toList());
        }
        boolean changed = source != null && source.revision != flow.inputRevision;
        if (changed) flow.revision++;
        if (!newcomers.isEmpty()) {
            JsonObject open = base(flow, "open");
            open.addProperty("kind", kind); open.addProperty("rows", 0); open.addProperty("size", 0);
            open.addProperty("name", actor.getGameProfile().name()); open.addProperty("yaw", flow.yaw);
            open.addProperty("side",side.name());
            open.addProperty("distance",distance);
            if(!panel.isEmpty())open.addProperty("panel",panel);
            open.addProperty("width", source == null ? 176 : source.width()); open.addProperty("height", source == null ? 64 : source.height());
            open.addProperty("uiRevision", Math.max(0, flow.revision - 1));
            send(open, newcomers);
            if (source == null) {
                JsonObject commit = base(flow, "commit"); commit.addProperty("revision", 1); send(commit, newcomers);
            } else scene(flow, source, new JsonArray[0], newcomers, true);
        }
        if (changed) {
            scene(flow, source, flow.previous, existing, false);
            flow.inputRevision = source.revision; flow.previous = source.commands();
        }
        boolean styleChanged=flow.cursorStyle!=cursorStyle;
        if (source != null && (flow.x != x || flow.y != y || !newcomers.isEmpty() || changed || styleChanged)) {
            JsonObject cursor = base(flow, "cursor"); cursor.addProperty("x", x); cursor.addProperty("y", y); cursor.addProperty("hover", -1);
            if(styleChanged||!newcomers.isEmpty()&&cursorStyle!=CursorStyle.ARROW)cursor.addProperty("cursorStyle",cursorStyle.id);
            send(cursor, watchers); flow.x = x; flow.y = y;
            flow.cursorStyle=cursorStyle;
        }
        flow.watchers = next;
    }
    private static void scene(Flow flow, GuiScene source, JsonArray[] previous, List<ServerPlayer> audience, boolean full) {
        if (audience.isEmpty()) return;
        for(boolean atomic:new boolean[]{false,true}) {
            List<ServerPlayer> group=audience.stream().filter(player->{var peer=NetworkSessions.peer(player);return (peer!=null&&peer.options.compact())==atomic;}).toList();
            if(group.isEmpty())continue;
            for(boolean copies:new boolean[]{false,true}) {
            var receivers=group.stream().filter(player->{var peer=NetworkSessions.peer(player);return (peer!=null&&peer.options.compact()&&peer.packetTemplates)==copies;}).toList();
            if(receivers.isEmpty())continue;
            for(JsonObject message:GuiScene.messages(flow.session,flow.revision,source.titleValue(),source.rect(),source.commands(),previous,source.carried(),full,atomic,copies)) {
                message.addProperty("actor",flow.owner.toString());send(message,receivers);
            }
            }
        }
    }
    public static void cleanup(MinecraftServer server, Set<UUID> active) {
        Iterator<Flow> iterator = flows.values().iterator();
        while (iterator.hasNext()) { Flow flow = iterator.next(); if (!active.contains(flow.owner)) { close(server, flow); iterator.remove(); } }
        sources.keySet().removeIf(id -> !active.contains(id));
    }
    public static void disconnect(ServerPlayer actor) {
        Flow flow = flows.remove(actor.getUUID());
        if (flow != null) close(actor.level().getServer(), flow);
        sources.remove(actor.getUUID());
        for (Flow other : flows.values()) other.watchers.remove(actor.getUUID());
    }
    public static void clear() { sources.clear(); flows.clear(); }
    public static void demand(ServerPlayer actor,long session,boolean needed){demand(actor,session,needed,false);}
    public static void demand(ServerPlayer actor, long session, boolean needed,boolean rich) {
        JsonObject packet = new JsonObject(); packet.addProperty("op", "ui_demand"); packet.addProperty("session", session); packet.addProperty("needed", needed);packet.addProperty("richText",rich);
        send(packet, List.of(actor));
    }
    private static JsonObject base(Flow flow, String op) {
        JsonObject packet = new JsonObject(); packet.addProperty("op", op); packet.addProperty("actor", flow.owner.toString()); packet.addProperty("session", flow.session); return packet;
    }
    private static void close(MinecraftServer server, Flow flow) { for (UUID id : flow.watchers) closeOne(server, flow, id); }
    private static void closeOne(MinecraftServer server, Flow flow, UUID id) {
        ServerPlayer observer = server.getPlayerList().getPlayer(id);
        if (observer != null) send(base(flow, "close"), List.of(observer));
    }
    private static void send(JsonObject message, List<ServerPlayer> audience) {
        String text = message.toString();
        if (text.length() > Wire.MAX_TEXT) throw new IllegalArgumentException("GUI payload budget");
        Wire.State payload=new Wire.State(text);
        for (ServerPlayer observer : audience) if (Platform.get().canSend(observer))
            NetworkBatch.send(observer, payload);
    }
    private static final class Flow {
        CursorStyle cursorStyle=CursorStyle.ARROW;
        final UUID owner; final long session; final String kind,panel; final float yaw; final GuiScene source;
        SharingRules.Side side;
        int distance;
        Set<UUID> watchers = new HashSet<>(); JsonArray[] previous = new JsonArray[0];
        long inputRevision, revision; float x = Float.NaN, y;
        Flow(ServerPlayer actor, long session, String kind, String panel, float yaw, GuiScene source) {
            owner = actor.getUUID(); this.session = session; this.kind = kind; this.panel=panel; this.source = source; this.yaw = yaw;
        }
    }
}
