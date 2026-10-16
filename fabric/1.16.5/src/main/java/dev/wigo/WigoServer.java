package dev.wigo;
import dev.wigo.protocol.NetworkBatch;

import com.google.gson.*;
import dev.wigo.protocol.Wire;
import dev.wigo.protocol.MirrorState;
import dev.wigo.protocol.NetworkControl;
import dev.wigo.protocol.NetworkSessions;
import dev.wigo.protocol.ProtocolText;
import dev.wigo.protocol.TextFallbacks;
import dev.wigo.platform.Platform;
import dev.wigo.config.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;
import java.util.*;

public final class WigoServer {
    private static final int MAX_PANELS = 8;
    private static final Map<UUID, Activity> activities = new HashMap<>();
    private static final Map<UUID, Flow> flows = new HashMap<>();

    public static void activity(ServerPlayer player, String text) {
        try {
            JsonObject json = new com.google.gson.JsonParser().parse(text).getAsJsonObject();
            if(NetworkControl.receive(player,json))return;
            if (json.get("version").getAsInt() != Wire.VERSION) return;
            Activity a = activities.computeIfAbsent(player.getUUID(), id -> new Activity());
            // At most two small activity messages per server tick.
            int tick = player.level.getServer().getTickCount();
            if (a.inputTick != tick) { a.inputTick = tick; a.inputCount = 0; }
            if (++a.inputCount > 2) return;
            String kind = json.get("kind").getAsString();
            if (!Set.of("none", "chest", "inventory", "gui", "unsupported").contains(kind)) return;
            long session = json.get("session").getAsLong();
            int window = json.get("window").getAsInt();
            float x = json.get("x").getAsFloat(), y = json.get("y").getAsFloat();
            if (session < 0 || !Float.isFinite(x) || !Float.isFinite(y)) return;
            int hovered = dev.wigo.protocol.LegacyClamp.clamp(json.get("hover").getAsInt(), -1, 89);
            boolean sharing = json.get("share").getAsBoolean(), receiving = json.get("receive").getAsBoolean();
            boolean smooth=json.has("smooth")&&json.get("smooth").getAsBoolean();
            int distance=json.has("distance")?SharingDistance.validate(json.get("distance").getAsInt()):SharingDistance.DEFAULT;
            boolean observers=json.has("observers")&&json.get("observers").getAsBoolean();
            CursorStyle cursorStyle=json.has("cursorStyle")?CursorStyle.fromId(json.get("cursorStyle").getAsInt()):a.cursorStyle;
            String panel=json.has("panel")?json.get("panel").getAsString():"";
            if(panel.length()>256)return;
            boolean newMenu = a.session != session || a.window != window || !a.kind.equals(kind);
            JsonElement title = newMenu ? null : a.title;
            if (json.has("title")) {
                var value = json.get("title");
                ProtocolText.validate(value,MirrorState.MAX_TITLE_CHARS);title=dev.wigo.protocol.LegacyJson.copy(value);
            }
            String menu=json.has("menu")?json.get("menu").getAsString():"";
            JsonObject labels=newMenu?null:a.labels;
            if(json.has("labels")) {
                labels=json.getAsJsonObject("labels");if(labels.size()>2)return;
                for(var entry:labels.entrySet()){if(!Set.of("inventory","crafting").contains(entry.getKey()))return;ProtocolText.validate(entry.getValue(),256);}
                labels=dev.wigo.protocol.LegacyJson.copy(labels);
            }
            if(!menu.isEmpty()&&!MenuCatalog.known(menu))return;
            long rules = json.has("rules") ? json.get("rules").getAsLong() : 0;
            if(rules < 0)return;
            if (newMenu) { a.drag = null; a.yaw = player.yRot; }
            a.kind = kind; a.session = session; a.window = window;
            a.x = dev.wigo.protocol.LegacyClamp.clamp(x, -16, 528); a.y = dev.wigo.protocol.LegacyClamp.clamp(y, -16, 400);
            a.hover = hovered; a.share = sharing; a.receive = receiving;
            a.distance=distance;
            a.cursorStyle=cursorStyle;
            var peer=NetworkSessions.peer(player);a.smooth=smooth&&peer!=null&&peer.smoothViewing;
            a.observers=observers&&peer!=null&&peer.observerCounts;
            a.panel=kind.equals("unsupported")?panel:"";
            a.title = kind.equals("chest") ? title : null;
            a.menu=menu;
            a.labels=labels;
            a.expectedRules=rules;
        } catch (RuntimeException ignored) { /* Invalid metadata never disconnects the game. */ }
    }
    public static void gui(ServerPlayer player, String text) {
        try {
            JsonObject packet=new com.google.gson.JsonParser().parse(text).getAsJsonObject();
            if(packet.has("op")&&packet.get("op").getAsString().equals("batch")) {
                var peer=NetworkSessions.peer(player);if(peer==null||!peer.options.compact()||!peer.packetTemplates)return;
                var parts=NetworkBatch.expandGui(packet);
                if(!packet.get("activity").isJsonNull())activity(player,packet.get("activity").toString());
                for(String part:parts)gui(player,part);return;
            }
            if(NetworkControl.receive(player,packet))return;
            if(TextFallbacks.receive(player,packet))return;
            if(packet.get("op").getAsString().equals("share_rules")) {
                Activity a=activities.computeIfAbsent(player.getUUID(),id->new Activity());
                int tick=player.level.getServer().getTickCount();
                if(a.rulesTick!=tick){a.rulesTick=tick;a.rulesCount=0;}
                if(++a.rulesCount>2)return;
                long revision=packet.get("revision").getAsLong();
                if(revision<1||revision<a.rulesRevision)return;
                SharingRules rules=SharingRules.parse(packet.getAsJsonObject("rules"));
                a.rules=rules;a.rulesRevision=revision;return;
            }
            Activity a = activities.get(player.getUUID());
            if (a == null || !a.share || !a.needed || !valid(player)) return;
            int tick = player.level.getServer().getTickCount();
            if (a.guiTick != tick) { a.guiTick = tick; a.guiCount = 0; }
            if (++a.guiCount > 12) return;
            if((a.kind.equals("chest")||a.kind.equals("inventory"))&&packet.get("op").getAsString().equals("drag")) {
                if(!valid(player)||packet.get("session").getAsLong()!=a.session||packet.get("window").getAsInt()!=a.window)return;
                int type=packet.get("type").getAsInt();var slots=packet.getAsJsonArray("slots");
                if(type<0||type>2||type==2&&!player.isCreative()||slots.size()>90)return;
                var indices=new LinkedHashSet<Integer>();
                for(var slot:slots) {int index=slot.getAsInt();if(index<0||index>=player.containerMenu.slots.size()||!indices.add(index))return;}
                a.drag=indices.isEmpty()?null:new DragInput(type,indices.stream().mapToInt(Integer::intValue).toArray());
            } else if(a.kind.equals("gui"))GuiRelay.receive(player,packet,a.session);
        } catch (RuntimeException ignored) { }
    }

    public static void tick(MinecraftServer server) {
        try(var batch=NetworkBatch.server()){tickMenus(server);}
    }
    private static void tickMenus(MinecraftServer server) {
        ServerMenus.tick(server);
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for(ServerPlayer player:players) {
            Activity a=activities.get(player.getUUID());
            if(a!=null&&a.policyRevision!=ServerMenus.revision()&&player.connection!=null&&Platform.get().canSend(player)) {
                JsonObject rules=new JsonObject();rules.addProperty("op","server_rules");
                JsonArray blocked=new JsonArray();ServerMenus.blocked().stream().sorted().forEach(blocked::add);rules.add("blocked",blocked);
                rules.addProperty("maxDistance",ServerMenus.maxMenuDistance());rules.addProperty("personalDistance",ServerMenus.allowPlayerDistance());
                rules.addProperty("observerCounter",ServerMenus.allowObserverCounter());rules.addProperty("smoothMode",ServerMenus.allowSmoothMode());
                send(rules,List.of(player));a.policyRevision=ServerMenus.revision();
            }
        }
        Map<UUID, List<ServerPlayer>> audience = new HashMap<>();
        for (ServerPlayer observer : players) {
            Activity watching = activities.get(observer.getUUID());
            if (watching == null || !watching.receive || !Platform.get().canSend(observer)) continue;
            players.stream().filter(actor -> valid(actor) && actor != observer && actor.level == observer.level
                            && inRange(actor,observer) && visibleTo(actor,observer))
                    .sorted(Comparator.comparingDouble(observer::distanceToSqr)).limit(MAX_PANELS)
                    .forEach(actor -> audience.computeIfAbsent(actor.getUUID(), ignored -> new ArrayList<>()).add(observer));
        }
        Set<UUID> active = new HashSet<>();
        Set<UUID> guiActive = new HashSet<>();
        for (ServerPlayer actor : players) {
            UUID id = actor.getUUID();
            List<ServerPlayer> watchers = audience.getOrDefault(id, List.of());
            Activity a = activities.get(id);
            if (a != null) {
                if(a.observers&&ServerMenus.allowObserverCounter()) {
                    int count=(int)watchers.stream().filter(p->(ServerMenus.countInvisibleObservers()||!p.hasEffect(MobEffects.INVISIBILITY))
                            &&(ServerMenus.countSpectatorObservers()||!p.isSpectator())).count();
                    if(a.observerSession!=a.session||a.observerCount!=count) {
                        JsonObject packet=new JsonObject();packet.addProperty("op","observers");packet.addProperty("session",a.session);packet.addProperty("count",count);
                        send(packet,List.of(actor));a.observerSession=a.session;a.observerCount=count;
                    }
                }else{a.observerSession=-1;a.observerCount=-1;}
                boolean needed = valid(actor) && !watchers.isEmpty();
                if(!needed)a.drag=null;
                var actorPeer=NetworkSessions.peer(actor);
                boolean rich=actorPeer!=null&&actorPeer.richText&&watchers.stream().allMatch(w->{var peer=NetworkSessions.peer(w);return peer!=null&&peer.richText;});
                if (a.demandSession != a.session || a.needed != needed || a.richText!=rich) {
                    if(a.demandSession==a.session&&a.richText!=rich)GuiRelay.invalidateSource(actor);
                    if (actor.connection != null) GuiRelay.demand(actor, a.session, needed,rich);
                    a.needed = needed;a.richText=rich; a.demandSession = a.session;
                }
            }
            if (!valid(actor) || watchers.isEmpty()) continue;
            if (a.kind.equals("gui") || a.kind.equals("unsupported")) {
                guiActive.add(id); GuiRelay.tick(actor, a.session, a.kind, a.panel, a.yaw, watchers, a.x, a.y,a.rules.side(),distance(actor),a.cursorStyle); continue;
            }
            active.add(id);
            Flow flow = flows.get(id);
            if (flow != null && (flow.session != a.session || flow.menu != actor.containerMenu)) {
                close(server, flow); flows.remove(id); flow = null;
            }
            if (flow == null) { flow = new Flow(actor, a); flows.put(id, flow); }
            Set<UUID> nextWatchers = new HashSet<>();
            for (ServerPlayer observer : watchers) nextWatchers.add(observer.getUUID());
            for (UUID old : flow.watchers) if (!nextWatchers.contains(old)) sendClose(server, flow, old);
            List<ServerPlayer> existing = new ArrayList<>();
            List<ServerPlayer> newcomers = new ArrayList<>();
            for (ServerPlayer observer : watchers) (flow.watchers.contains(observer.getUUID()) ? existing : newcomers).add(observer);
            int distance=distance(actor);
            if(flow.distance!=distance) {
                flow.distance=distance;JsonObject changed=base(flow,"distance");changed.addProperty("distance",distance);send(changed,existing);
            }
            if(flow.side!=a.rules.side()) {
                flow.side=a.rules.side();JsonObject side=base(flow,"side");side.addProperty("side",flow.side.name());
                send(side,existing.stream().filter(p->{var peer=NetworkSessions.peer(p);return peer!=null&&peer.smoothViewing;}).toList());
            }
            update(flow, actor, a.title, existing, newcomers);
            sendDrag(flow,actor,a.drag,existing,newcomers);
            boolean styleChanged=flow.cursorStyle!=a.cursorStyle;
            boolean cursorChanged = flow.x != a.x || flow.y != a.y || flow.hover != a.hover || styleChanged;
            if (cursorChanged || !newcomers.isEmpty()) {
                JsonObject cursor = base(flow, "cursor");
                cursor.addProperty("x", a.x); cursor.addProperty("y", a.y); cursor.addProperty("hover", a.hover);
                if(styleChanged||!newcomers.isEmpty()&&a.cursorStyle!=CursorStyle.ARROW)cursor.addProperty("cursorStyle",a.cursorStyle.id);
                send(cursor, cursorChanged ? watchers : newcomers);
                flow.x = a.x; flow.y = a.y; flow.hover = a.hover;
                flow.cursorStyle=a.cursorStyle;
            }
            flow.watchers = nextWatchers;
        }
        Iterator<Flow> iterator = flows.values().iterator();
        while (iterator.hasNext()) {
            Flow flow = iterator.next();
            if (!active.contains(flow.owner)) { close(server, flow); iterator.remove(); }
        }
        GuiRelay.cleanup(server, guiActive);
    }

    private static int distance(ServerPlayer owner) {
        Activity a=activities.get(owner.getUUID());
        return SharingDistance.effective(a==null?SharingDistance.DEFAULT:a.distance,ServerMenus.maxMenuDistance(),ServerMenus.allowPlayerDistance());
    }
    private static boolean inRange(ServerPlayer owner,ServerPlayer observer) {
        int range=distance(owner);return owner.distanceToSqr(observer)<=range*range;
    }
    private static boolean valid(ServerPlayer player) {
        Activity a = activities.get(player.getUUID());
        if (a == null || !a.share || a.expectedRules!=a.rulesRevision || player.isSpectator() || a.window != player.containerMenu.containerId) return false;
        if(a.kind.equals("unsupported")&&!ServerMenus.allows(MenuCatalog.UNSUPPORTED))return false;
        if(a.kind.equals("unsupported")&&!a.rules.allowsMenu(MenuCatalog.UNSUPPORTED))return false;
        if(!a.menu.isEmpty()&&!ServerMenus.allows(a.menu))return false;
        if(!a.menu.isEmpty()&&!a.rules.allowsMenu(a.menu))return false;
        if(player.containerMenu!=player.inventoryMenu||a.kind.equals("inventory")||a.kind.equals("chest")) {
            String actual=MenuTypes.id(player.containerMenu);
            if(!ServerMenus.allows(actual)||!a.rules.allowsMenu(actual))return false;
        } else if(a.menu.isEmpty()&&!ServerMenus.allowsUnclassified())return false;
        return a.kind.equals("chest") && player.containerMenu instanceof ChestMenu
                || a.kind.equals("inventory") && player.containerMenu == player.inventoryMenu
                || a.kind.equals("gui") || a.kind.equals("unsupported");
    }
    private static boolean visibleTo(ServerPlayer actor,ServerPlayer observer) {
        Activity activity = activities.get(actor.getUUID());
        Activity watching=activities.get(observer.getUUID());
        SharingRules rules = activity.rules;
        double forward = actor.isSleeping() && activity.menu.equals("minecraft:chat") ? 0 : SharingRules.PANEL_FORWARD_OFFSET;
        return rules.allowsPlayer(observer.getUUID(),observer.getGameProfile().getName())
                && (ServerMenus.allowSmoothMode()&&watching!=null&&watching.smooth||rules.allowsPosition(observer.getX()-actor.getX(),observer.getZ()-actor.getZ(),activity.yaw,forward));
    }
    public static boolean mayObserve(ServerPlayer actor,ServerPlayer observer) {
        Activity watching=activities.get(observer.getUUID());
        return actor!=observer&&watching!=null&&watching.receive&&valid(actor)&&actor.level==observer.level
                &&inRange(actor,observer)&&visibleTo(actor,observer);
    }
    public static boolean hasCaption(ServerPlayer actor,JsonElement reference) {
        Flow flow=flows.get(actor.getUUID());
        if(flow!=null&&(Objects.equals(flow.title,reference)||flow.labels!=null&&flow.labels.entrySet().stream().anyMatch(entry->entry.getValue().equals(reference))))return true;
        return GuiRelay.hasCaption(actor.getUUID(),reference);
    }

    private static void sendDrag(Flow flow,ServerPlayer actor,DragInput input,List<ServerPlayer> existing,List<ServerPlayer> newcomers) {
        ItemStack held=actor.inventory.getCarried();var selected=new LinkedHashSet<Slot>();
        if(input!=null&&!held.isEmpty())for(int index:input.slots) {
            Slot slot=flow.menu.getSlot(index);
            if(AbstractContainerMenu.canItemQuickReplace(slot,held,true)&&flow.menu.canDragTo(slot)&&slot.mayPlace(held))selected.add(slot);
        }
        var counts=new JsonArray();int remainder=held.getCount();
        if(!selected.isEmpty()) {
            if(input.type==2)remainder=held.getMaxStackSize();
            for(var slot:selected) {
                int before=slot.getItem().getCount(),count=selected.size()==1?0:Math.min(
                        Math.min(held.getMaxStackSize(),slot.getMaxStackSize(held)),
                        dev.wigo.protocol.LegacyQuickCraft.placeCount(selected,input.type,held)+before);
                var entry=new JsonArray();entry.add(slot.index);entry.add(count);counts.add(entry);
                if(selected.size()>1&&input.type!=2)remainder-=count-before;
            }
        }
        if((counts.size()==0)&&flow.lastDrag==null)return;
        JsonObject message=base(flow,"drag");message.addProperty("revision",flow.revision);
        message.add("slots",counts);message.addProperty("carriedCount",Math.max(0,remainder));
        String value=message.toString();
        if(!value.equals(flow.lastDrag))send(message,existing);
        if(!(counts.size()==0)&&!newcomers.isEmpty())send(message,newcomers);
        flow.lastDrag=(counts.size()==0)?null:value;
    }

    private static void update(Flow flow, ServerPlayer actor, JsonElement title, List<ServerPlayer> existing, List<ServerPlayer> newcomers) {
        JsonObject changed = new JsonObject();
        for (int i = 0; i < flow.items.length; i++) {
            ItemStack item = flow.menu.getSlot(i).getItem();
            if (!ItemStack.matches(item, flow.items[i]) || flow.revision == 0) {
                flow.items[i] = item.copy();
                flow.encoded[i] = StackJson.encode(item, actor.level.registryAccess());
                changed.add(Integer.toString(i), flow.encoded[i]);
            }
        }
        boolean carriedChanged = !ItemStack.matches(flow.carried, actor.inventory.getCarried()) || flow.revision == 0;
        if (carriedChanged) {
            flow.carried = actor.inventory.getCarried().copy();
            flow.encodedCarried = StackJson.encode(flow.carried, actor.level.registryAccess());
        }
        boolean titleChanged = !Objects.equals(flow.title, title);
        if (titleChanged) flow.title = title;
        if (changed.size()!=0 || carriedChanged || titleChanged) {
            flow.revision++;
            transmitUpdate(flow,changed,carriedChanged,titleChanged,existing);
        }
        if (!newcomers.isEmpty()) {
            JsonObject open = base(flow, "open");
            open.addProperty("kind", flow.kind); open.addProperty("rows", flow.rows);
            open.addProperty("distance",flow.distance);
            open.addProperty("size", flow.items.length); open.addProperty("name", actor.getGameProfile().getName());
            open.addProperty("yaw", flow.yaw);
            open.addProperty("side",flow.side.name());
            if (flow.title != null) open.add("title", flow.title);
            if(flow.labels!=null)open.add("labels",flow.labels);
            open.addProperty("revision", flow.revision - 1);
            send(open, newcomers);
            // A new observer gets a complete snapshot of the current global revision.
            JsonObject all = new JsonObject();
            for (int i = 0; i < flow.encoded.length; i++) all.add(Integer.toString(i), flow.encoded[i]);
            transmitUpdate(flow,all,true,false,newcomers);
        }
    }
    private static void transmitUpdate(Flow flow,JsonObject slots,boolean carry,boolean title,List<ServerPlayer> audience) {
        if(audience.isEmpty())return;
        JsonObject update=base(flow,"update");update.addProperty("revision",flow.revision);update.add("slots",slots);
        if(carry)update.add("carried",flow.encodedCarried);if(title&&flow.title!=null)update.add("title",flow.title);
        boolean fits=update.toString().length()<=Wire.MAX_TEXT-128;
        List<ServerPlayer> compact=new ArrayList<>(),legacy=new ArrayList<>();
        for(ServerPlayer player:audience){var peer=NetworkSessions.peer(player);(fits&&peer!=null&&peer.options.compact()?compact:legacy).add(player);}
        send(update,compact);sendPatch(flow,slots,legacy);sendCommit(flow,flow.revision,carry,title,legacy);
    }

    private static void sendPatch(Flow flow, JsonObject slots, List<ServerPlayer> watchers) {
        sendPatch(flow, slots, watchers, flow.revision);
    }
    private static void sendPatch(Flow flow, JsonObject slots, List<ServerPlayer> watchers, long revision) {
        if (watchers.isEmpty() || slots.size()==0) return;
        JsonObject batch = new JsonObject();
        int size = 0;
        for (var entry : slots.entrySet()) {
            int length = entry.getValue().toString().length() + 12;
            if (size + length > 12000 && batch.size()!=0) {
                patchBatch(flow, batch, watchers, revision); batch = new JsonObject(); size = 0;
            }
            batch.add(entry.getKey(), entry.getValue()); size += length;
        }
        if (batch.size()!=0) patchBatch(flow, batch, watchers, revision);
    }
    private static void patchBatch(Flow flow, JsonObject batch, List<ServerPlayer> watchers, long revision) {
        JsonObject message = base(flow, "patch");
        message.addProperty("revision", revision);
        message.add("slots", batch);
        send(message, watchers);
    }
    private static void sendCommit(Flow flow, long revision, boolean carry, boolean title, List<ServerPlayer> watchers) {
        JsonObject message = base(flow, "commit");
        message.addProperty("revision", revision);
        if (carry) message.add("carried", flow.encodedCarried);
        if (title&&flow.title!=null) message.add("title", flow.title);
        send(message, watchers);
    }
    private static JsonObject base(Flow flow, String operation) {
        JsonObject json = new JsonObject();
        json.addProperty("op", operation); json.addProperty("actor", flow.owner.toString());
        json.addProperty("session", flow.session);
        return json;
    }
    private static void send(JsonObject json, List<ServerPlayer> watchers) {
        if (watchers.isEmpty()) return;
        String text = json.toString();
        if (text.length() > Wire.MAX_TEXT) throw new IllegalStateException("Oversized WIGO message");
        Wire.State payload = new Wire.State(text);
        for (ServerPlayer watcher : watchers) if (Platform.get().canSend(watcher)) NetworkBatch.send(watcher, payload);
    }
    private static void sendClose(MinecraftServer server, Flow flow, UUID observer) {
        ServerPlayer player = server.getPlayerList().getPlayer(observer);
        if (player != null) send(base(flow, "close"), List.of(player));
    }
    private static void close(MinecraftServer server, Flow flow) {
        for (UUID observer : flow.watchers) sendClose(server, flow, observer);
    }
    public static void disconnect(ServerPlayer player) {
        GuiRelay.disconnect(player);
        Flow flow = flows.remove(player.getUUID());
        if (flow != null) close(player.level.getServer(), flow);
        activities.remove(player.getUUID());
        TextFallbacks.disconnect(player.getUUID());
        for (Flow other : flows.values()) other.watchers.remove(player.getUUID());
    }
    public static void resnapshot(ServerPlayer observer) {
        for(Flow flow:flows.values())flow.watchers.remove(observer.getUUID());
        GuiRelay.resnapshot(observer);
    }
    public static void clear() { flows.clear(); activities.clear(); GuiRelay.clear(); ServerMenus.clear();TextFallbacks.clear(); }

    private static final class Activity {
        CursorStyle cursorStyle=CursorStyle.ARROW;
        SharingRules rules=SharingRules.defaults();
        long expectedRules,rulesRevision,policyRevision=-1;
        int rulesTick=-1,rulesCount;
        DragInput drag;
        String kind = "none",panel="",menu=""; long session; int window, hover = -1, inputTick = -1, inputCount;
        JsonElement title;
        JsonObject labels;
        float x = -16, y = -16, yaw; boolean share = true, receive = true, smooth;
        int distance=SharingDistance.DEFAULT,observerCount=-1;boolean observers;long observerSession=-1;
        boolean needed,richText; long demandSession = -1; int guiTick = -1, guiCount;
    }
    private static final class Flow {
        CursorStyle cursorStyle=CursorStyle.ARROW;
        String lastDrag;
        JsonElement title;
        final JsonObject labels;
        final UUID owner; final long session; final AbstractContainerMenu menu;
        final String kind; final int rows; final float yaw;
        SharingRules.Side side;
        int distance;
        final ItemStack[] items; final JsonElement[] encoded;
        ItemStack carried = ItemStack.EMPTY; JsonElement encodedCarried = JsonNull.INSTANCE;
        long revision; float x = Float.NaN, y; int hover = -1;
        Set<UUID> watchers = new HashSet<>();
        Flow(ServerPlayer player, Activity a) {
            owner = player.getUUID(); session = a.session; menu = player.containerMenu;
            title = a.title;
            side=a.rules.side();
            distance=distance(player);
            labels=a.labels;
            kind = a.kind; rows = menu instanceof ChestMenu chest ? chest.getRowCount() : 0; yaw = a.yaw;
            items = new ItemStack[menu.slots.size()]; Arrays.fill(items, ItemStack.EMPTY);
            encoded = new JsonElement[items.length]; Arrays.fill(encoded, JsonNull.INSTANCE);
        }
    }
    private record DragInput(int type,int[] slots) {}
}
