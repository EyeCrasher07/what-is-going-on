package dev.wigo.client;
import dev.wigo.protocol.NetworkBatch;

import com.google.gson.*;
import dev.wigo.Wigo;
import dev.wigo.config.MenuCatalog;
import dev.wigo.mixin.ContainerScreenAccessor;
import dev.wigo.protocol.Wire;
import dev.wigo.protocol.MirrorState;
import dev.wigo.protocol.NetworkSessions;
import dev.wigo.protocol.NetworkControl;
import dev.wigo.protocol.WireDictionary;
import dev.wigo.protocol.ProtocolText;
import dev.wigo.platform.Platform;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ChestMenu;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class WigoClient {
    public static final Map<UUID, RemoteMenu> menus = new LinkedHashMap<>();
    public static long receivedBytes;
    private static Screen lastScreen;
    private static long session;
    private static String lastActivity;
    private static String lastTitle;
    private static long lastTitleSession = -1;
    private static String lastKind = "none";
    private static int ticks;
    private static long sentRules=-1;
    private static int lastCursorStyle=-1;
    private static Set<String> blockedMenus=Set.of();
    public static boolean serverAllows(String id) {return !blockedMenus.contains(id);}
    private static KeyMapping settingsKey;
    public static boolean connected;
    public static int observerCount;
    private static int maxSharingDistance=dev.wigo.config.SharingDistance.DEFAULT;
    private static boolean personalDistance=true;
    private static boolean observerCounterAllowed=true,smoothModeAllowed=true;
    public static boolean allowsObserverCounter(){return observerCounterAllowed;}
    public static boolean allowsSmoothMode(){return smoothModeAllowed;}
    public static int maxSharingDistance(){return maxSharingDistance;}
    public static boolean personalDistance(){return personalDistance;}
    public static int sharingDistance(){return dev.wigo.config.SharingDistance.effective(Settings.sharingDistance,maxSharingDistance,personalDistance);}
    public static boolean supportsObserverCounts(){var peer=networkPeer();return peer!=null&&peer.observerCounts;}
    private static long helloTime,serverOffset;
    private static NetworkSessions.Peer networkPeer() {
        var connection=Minecraft.getInstance().getConnection();return connection==null?null:NetworkSessions.peer(connection.getConnection());
    }
    public static boolean richText(){var peer=networkPeer();return peer!=null&&peer.richText;}
    public static boolean compactMode(){var peer=networkPeer();return peer!=null&&peer.options.compact();}
    public static boolean packetTemplates(){var peer=networkPeer();return peer!=null&&peer.options.compact()&&peer.packetTemplates;}
    public static void enabled(boolean value) {
        Settings.enabled=value;Settings.save();lastActivity=null;
        menus.clear();PanelRenderer.close();observerCount=0;
        if(!value){GuiCapture.reset();DragPreview.reset();}
    }
    public static String networkFormatKey() {
        if(!connected)return "wigo.info.format.disconnected";
        var peer=networkPeer();
        if(peer==null||!peer.negotiated)return "wigo.info.format.pending";
        return peer.options.compact()?(peer.options.translations()?"wigo.info.format.translation_ids":"wigo.info.format.compact"):"wigo.info.format.legacy";
    }
    public static boolean supportsSmoothMenus(){var peer=networkPeer();return peer!=null&&peer.smoothViewing;}
    public static boolean smoothMenus(){return Settings.smoothMenus&&smoothModeAllowed&&supportsSmoothMenus();}
    public static long serverTime(){return System.currentTimeMillis()+serverOffset;}
    public static Wire.Gui guiPayload(String text){return new Wire.Gui(sourceText(text,Wire.MAX_TEXT));}
    private static String sourceText(String text,int maxChars) {
        var peer=networkPeer();if(peer==null||!peer.options.translations()||!peer.richSource)return text;
        String encoded=ProtocolText.encode(JsonParser.parseString(text),true,peer.richText&&peer.richSource).toString();return encoded.length()<=maxChars?encoded:text;
    }

    private static LocalTranslations localTranslations;
    private static final Map<UUID,List<String>> waitingTexts=new HashMap<>();
    private static final Map<UUID,Long> waitingSince=new HashMap<>();
    private static final Set<String> requestedTexts=new HashSet<>();
    public static void resourcesReloaded(){
        if(localTranslations!=null)localTranslations.reload();
        TextProvenance.clear();GuiCapture.resync();PanelRenderer.close();
        CursorSprites.clear();
        if(connected&&Minecraft.getInstance().level!=null)resetIncoming();
    }
    public static void initialize() {
        Settings.load();
        UpdateChecker.start();
        localTranslations=new LocalTranslations();ProtocolText.provider(localTranslations);
        settingsKey = new KeyMapping("key.wigo.settings", InputConstants.KEY_F8,
                "key.category.wigo.controls");
    }
    public static KeyMapping settingsKey() { return settingsKey; }
    public static void joined() {
        reset(); connected=Platform.get().canSendActivity();
        var peer=networkPeer();if(peer!=null)peer.clientSide=true;
        negotiate();
        if(!connected) Wigo.LOG.info("Server has no compatible WIGO channel; install WIGO on the server or LAN host");
    }
    public static void disconnected() { reset(); }

    public static void receive(String text) {
        receive(text,true);
    }
    private static void receive(String text,boolean countBytes) {
        Minecraft mc = Minecraft.getInstance();
        if(countBytes)receivedBytes += text.getBytes(StandardCharsets.UTF_8).length;
        try {
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            String op = json.get("op").getAsString();
            if(op.equals("batch")){for(String part:NetworkBatch.expandStates(json))receive(part,false);return;}
            if(op.equals("network_ready")) {
                var peer=networkPeer();if(peer==null||peer.negotiated)return;
                boolean compact=NetworkControl.compatible(json);
                peer.smoothViewing=json.has("smooth")&&json.get("smooth").getAsBoolean();
                peer.observerCounts=json.has("observers")&&json.get("observers").getAsBoolean();
                peer.shortText=compact&&dev.wigo.protocol.ShortTextDictionary.supports(json);
                peer.richText=compact&&json.has("sceneFormat")&&json.get("sceneFormat").getAsInt()==2;
                peer.packetTemplates=compact&&NetworkControl.supportsPacketTemplates(json);
                peer.negotiate(new NetworkSessions.Options(compact,compact&&json.get("translations").getAsBoolean()));
                if(json.has("serverTime"))serverOffset=json.get("serverTime").getAsLong()+(System.currentTimeMillis()-helloTime)/2-System.currentTimeMillis();
                return;
            }
            if(mc.level==null)return;
            if(op.equals("observers")) {
                int count=json.get("count").getAsInt();
                if(observerCounterAllowed&&Settings.showObservers&&Settings.sharing()&&json.get("session").getAsLong()==session
                        &&count>=0&&count<=1000000)observerCount=count;
                return;
            }
            if(op.equals("network_reset")) {var peer=networkPeer();if(peer!=null)peer.resetEncoder();GuiCapture.resync();lastActivity=null;return;}
            if(op.equals("decode_error")) {resetIncoming();return;}
            if(op.equals("text_request")) {
                var ref=json.get("reference");ProtocolText.validate(ref,2048);var parts=ref.getAsJsonObject().getAsJsonArray("$text");
                String literal=localTranslations.resolve(parts.get(0).getAsInt(),parts.get(1).getAsInt(),parts.get(2).getAsJsonArray(),parts.get(3).getAsBoolean());
                JsonObject reply=new JsonObject();reply.addProperty("op","text_reply");reply.add("request",json.get("request"));reply.addProperty("value",literal);
                Platform.get().send(Wire.Gui.legacy(reply.toString()));return;
            }
            if(op.equals("text_reply")) {
                var ref=json.get("reference");ProtocolText.validate(ref,2048);var value=json.get("value");
                if(!ProtocolText.isReference(ref)||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())return;
                String literal=value.getAsString();if(literal.length()>2048)return;
                UUID actor=UUID.fromString(json.get("actor").getAsString());if(!requestedTexts.remove(actor+":"+ref))return;
                localTranslations.fallback(ref,literal);
                List<String> queued=waitingTexts.remove(actor);waitingSince.remove(actor);
                if(queued!=null)for(String queuedText:queued)receive(queuedText);
                return;
            }
            if(json.has("actor")) {
                UUID actor=UUID.fromString(json.get("actor").getAsString());
                if(op.equals("close")){waitingTexts.remove(actor);waitingSince.remove(actor);requestedTexts.removeIf(key->key.startsWith(actor+":"));}
                else if(waitingTexts.containsKey(actor)){queueText(actor,text);return;}
            }
            json=ProtocolText.resolve(json).getAsJsonObject();
            if (op.equals("ui_demand")) { GuiCapture.demand(json.get("session").getAsLong(), json.get("needed").getAsBoolean(),json.has("richText")&&json.get("richText").getAsBoolean()); return; }
            if (op.equals("ui_resync")) { if (json.get("session").getAsLong() == session) GuiCapture.resync(); return; }
            if (op.equals("server_rules")) {
                JsonArray blocked=json.getAsJsonArray("blocked");
                if(blocked.size()>MenuCatalog.menus().size())return;
                Set<String> next=new HashSet<>();
                for(var value:blocked) {String id=value.getAsString();if(!MenuCatalog.known(id))return;next.add(id);}
                int maximum=json.has("maxDistance")?dev.wigo.config.SharingDistance.validate(json.get("maxDistance").getAsInt()):dev.wigo.config.SharingDistance.DEFAULT;
                boolean personal=!json.has("personalDistance")||json.get("personalDistance").getAsBoolean();
                boolean counter=!json.has("observerCounter")||json.get("observerCounter").getAsBoolean();
                boolean smooth=!json.has("smoothMode")||json.get("smoothMode").getAsBoolean();
                blockedMenus=Set.copyOf(next);maxSharingDistance=maximum;personalDistance=personal;
                observerCounterAllowed=counter;smoothModeAllowed=smooth;
                if(!counter)observerCount=0;
                return;
            }
            if (!Settings.receiving()) return;
            UUID owner = UUID.fromString(json.get("actor").getAsString());
            if (op.equals("close")) {
                RemoteMenu menu = menus.get(owner);
                if (menu != null && menu.state.session == json.get("session").getAsLong()) menus.remove(owner);
                return;
            }
            if (op.equals("open")) {
                if (!menus.containsKey(owner) && menus.size() >= 8) return;
                RemoteMenu next = new RemoteMenu(owner);
                next.accept(json, mc.level.registryAccess());
                menus.put(owner, next);
                return;
            }
            RemoteMenu menu = menus.get(owner);
            if (menu != null) menu.accept(json, mc.level.registryAccess());
        } catch(LocalTranslations.Missing missing) {
            try {
                var packet=JsonParser.parseString(text).getAsJsonObject();if(!packet.has("actor"))return;
                UUID actor=UUID.fromString(packet.get("actor").getAsString());if(!queueText(actor,text))return;
                String key=actor+":"+missing.reference;if(requestedTexts.contains(key))return;
                if(requestedTexts.size()>=32){resetIncoming();return;}requestedTexts.add(key);
                JsonObject request=new JsonObject();request.addProperty("op","text_request");request.addProperty("actor",actor.toString());request.add("reference",missing.reference);
                Platform.get().send(Wire.Gui.legacy(request.toString()));
            }catch(RuntimeException ignored){}
        } catch (RuntimeException e) {
            // Packet validation is isolated from the vanilla connection.
            Wigo.LOG.debug("Ignored invalid WIGO state: {}", e.toString());
        }
    }
    private static boolean queueText(UUID actor,String text) {
        if(waitingTexts.size()>=8&&!waitingTexts.containsKey(actor)){resetIncoming();return false;}
        var queue=waitingTexts.computeIfAbsent(actor,key->new ArrayList<>());
        if(queue.size()>=16){resetIncoming();return false;}
        waitingSince.putIfAbsent(actor,System.currentTimeMillis());queue.add(text);return true;
    }
    private static void resetIncoming() {
        waitingTexts.clear();waitingSince.clear();requestedTexts.clear();menus.clear();
        Platform.get().send(Wire.Gui.legacy("{\"op\":\"network_reset\"}"));
    }

    public static void tick(Minecraft mc) {
        try(var batch=NetworkBatch.client(packetTemplates())){tickMenus(mc);}
    }
    private static void tickMenus(Minecraft mc) {
        while (settingsKey.consumeClick()) {
            Screen previous = mc.screen;
            if (WigoLinks.owns(previous)) { mc.setScreen(WigoLinks.previous(previous)); continue; }
            if (previous instanceof AbstractContainerScreen<?>) {
                previous.onClose();
                previous = mc.screen;
            }
            mc.setScreen(new WigoHomeScreen(previous));
        }
        if (mc.player == null || mc.level == null || !Platform.get().canSendActivity()) return;
        if(waitingSince.values().stream().anyMatch(since->System.currentTimeMillis()-since>10000))resetIncoming();
        connected = true;
        negotiate();
        if(sentRules!=Settings.rulesRevision()&&Platform.get().canSendGui()) {
            JsonObject rules=new JsonObject();rules.addProperty("op","share_rules");rules.addProperty("revision",Settings.rulesRevision());
            rules.add("rules",Settings.rules().json());Platform.get().send(new Wire.Gui(rules.toString()));sentRules=Settings.rulesRevision();
        }
        Screen screen = mc.screen;
        String kind = Settings.sharing() ? GuiCapture.classify(screen) : "none";
        String menu=MenuIds.of(screen,kind);
        if(!menu.isEmpty()&&(!Settings.menuEnabled(menu)||!serverAllows(menu))) {kind="none";menu="";}
        if (screen != lastScreen || !kind.equals(lastKind)) {
            lastScreen = screen; lastKind = kind; session++; GuiCapture.session(session);observerCount=0;
        }
        String title = kind.equals("chest") ? containerTitle(screen) : null;
        boolean titleChanged = session != lastTitleSession || !Objects.equals(title, lastTitle);
        int window = mc.player.containerMenu.containerId;
        float x = -16, y = -16;
        int hover = -1;
        if (kind.equals("gui")) {
            float[] point = GuiCapture.pointer((float)mc.mouseHandler.getScaledXPos(mc.getWindow()), (float)mc.mouseHandler.getScaledYPos(mc.getWindow()));
            x = point[0]; y = point[1];
        } else if (kind.equals("chest") || kind.equals("inventory")) {
            if (screen instanceof AbstractContainerScreen<?> container) {
                ContainerScreenAccessor bounds = (ContainerScreenAccessor)container;
                x = (float)(mc.mouseHandler.getScaledXPos(mc.getWindow()) - bounds.wigoLeft());
                y = (float)(mc.mouseHandler.getScaledYPos(mc.getWindow()) - bounds.wigoTop());
                x = Math.clamp(x, -16, 192); y = Math.clamp(y, -16, bounds.wigoHeight() + 16);
                for (int i = 0; i < container.getMenu().slots.size(); i++) {
                    var slot = container.getMenu().getSlot(i);
                    if (x >= slot.x - 1 && x < slot.x + 17 && y >= slot.y - 1 && y < slot.y + 17) { hover = i; break; }
                }
                window = container.getMenu().containerId;
            }
        }
        JsonObject json = new JsonObject();
        json.addProperty("version", Wire.VERSION); json.addProperty("kind", kind);
        if(!kind.equals("none"))json.addProperty("menu",menu);
        if(kind.equals("unsupported"))json.addProperty("panel",GuiCapture.panelName(screen));
        json.addProperty("session", session); json.addProperty("window", window);
        json.addProperty("x", Math.round(x * 4) / 4f); json.addProperty("y", Math.round(y * 4) / 4f);
        json.addProperty("hover", hover); json.addProperty("share", Settings.sharing()); json.addProperty("receive", Settings.receiving());
        json.addProperty("smooth",smoothMenus());
        if(observerCounterAllowed&&Settings.showObservers&&Settings.enabled&&supportsObserverCounts())json.addProperty("observers",true);
        if(sharingDistance()!=dev.wigo.config.SharingDistance.DEFAULT)json.addProperty("distance",sharingDistance());
        json.addProperty("rules",Settings.rulesRevision());
        String text = json.toString();
        boolean cursorChanged=lastCursorStyle!=Settings.cursorStyle.id;
        if (!text.equals(lastActivity) || titleChanged || cursorChanged || ++ticks >= 100) {
            if(cursorChanged)json.addProperty("cursorStyle",Settings.cursorStyle.id);
            // Titles are session metadata, not part of every cursor update.
            if (title != null && titleChanged){json.addProperty("title", title);if(!(screen.getTitle().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents))json.addProperty("literalTitle",true);}
            var peer=networkPeer();
            if(titleChanged&&peer!=null&&peer.options.translations()&&(kind.equals("inventory")||kind.equals("chest"))) {
                JsonObject labels=new JsonObject();labels.addProperty("inventory",visualCaption("container.inventory"));
                if(kind.equals("inventory"))labels.addProperty("crafting",visualCaption("container.crafting"));
                if(labels.toString().length()<=512&&labels.entrySet().stream().allMatch(entry->entry.getValue().getAsString().length()<=256)) {
                    json.add("labels",labels);if(json.toString().length()>1024)json.remove("labels");
                }
            }
            NetworkBatch.send(new Wire.Activity(sourceText(json.toString(),1024)));
            lastActivity = text; lastTitle = title; lastTitleSession = session; ticks = 0;
            lastCursorStyle=Settings.cursorStyle.id;
        }
        if (!Settings.receiving()) menus.clear();
        GuiCapture.tick();
        DragPreview.tick(screen,kind,session);
    }
    public static long currentSession() { return session; }
    private static String visualCaption(String key){StringBuilder text=new StringBuilder();Component.translatable(key).getVisualOrderText().accept((i,style,p)->{text.appendCodePoint(p);return true;});return text.toString();}
    private static void negotiate() {
        var peer=networkPeer();if(peer==null||peer.helloSent||!Platform.get().canSendGui())return;
        peer.helloSent=true;helloTime=System.currentTimeMillis();Platform.get().send(Wire.Gui.legacy(NetworkControl.hello().toString()));
    }

    private static String containerTitle(Screen screen) {
        StringBuilder visual=new StringBuilder();screen.getTitle().copy().getVisualOrderText().accept((i,style,p)->{visual.appendCodePoint(p);return true;});
        String title = visual.toString();
        int end = Math.min(title.length(), MirrorState.MAX_TITLE_CHARS);
        // Leave room in the small activity frame, including JSON escapes.
        while (end > 0 && (Character.isHighSurrogate(title.charAt(end - 1))
                || new JsonPrimitive(title.substring(0, end)).toString().length() > 512)) end--;
        return title.substring(0, end);
    }

    public static void reset() {
        DragPreview.reset();
        menus.clear(); PanelRenderer.close(); GuiCapture.reset();
        lastScreen = null; lastActivity = null; lastKind = "none"; session = 0; ticks = 0; receivedBytes = 0; connected = false;
        lastTitle = null; lastTitleSession = -1;
        sentRules=-1;blockedMenus=Set.of();
        lastCursorStyle=-1;
        helloTime=serverOffset=0;
        observerCount=0;maxSharingDistance=dev.wigo.config.SharingDistance.DEFAULT;personalDistance=true;
        observerCounterAllowed=smoothModeAllowed=true;
        if(localTranslations!=null)localTranslations.reset();
        waitingTexts.clear();waitingSince.clear();requestedTexts.clear();
    }
}
