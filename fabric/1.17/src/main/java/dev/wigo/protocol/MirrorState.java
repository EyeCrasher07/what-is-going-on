package dev.wigo.protocol;

import com.google.gson.*;
import java.util.Arrays;
import java.util.Set;
import dev.wigo.config.SharingRules;
import dev.wigo.config.CursorStyle;

/** A bounded, atomic mirror. Contains data only; never touches a real inventory. */
public final class MirrorState {
    public static final int MAX_SLOTS = 90;
    public static final int MAX_STACK_CHARS = 2048;
    public static final int MAX_TITLE_CHARS = 256;
    public long session, revision;
    public String kind, name;
    public String panel="";
    public String title;
    public String inventoryCaption,craftingCaption;
    public int rows, slotCount;
    public int guiWidth = 176, guiHeight = 166;
    public float yaw, mouseX = -16, mouseY = -16;
    public SharingRules.Side side=SharingRules.Side.BOTH;
    public int distance=dev.wigo.config.SharingDistance.DEFAULT;
    public int hovered = -1;
    public CursorStyle cursorStyle = CursorStyle.ARROW;
    public boolean ready;
    private JsonElement[] slots;
    private JsonElement[] pending;
    private long pendingRevision = -1;
    private JsonElement carried = JsonNull.INSTANCE;

    public boolean accept(JsonObject message) {
        String op = message.get("op").getAsString();
        if (op.equals("open")) {
            String nextKind = message.get("kind").getAsString();
            int nextRows = message.get("rows").getAsInt();
            int count = message.get("size").getAsInt();
            if (!(nextKind.equals("chest") && nextRows >= 1 && nextRows <= 6 && count == nextRows * 9 + 36)
                    && !(nextKind.equals("inventory") && nextRows == 0 && count == 46)
                    && !(Set.of("gui", "unsupported").contains(nextKind) && nextRows == 0 && count == 0))
                throw new IllegalArgumentException("Unsupported menu shape");
            long nextSession = message.get("session").getAsLong();
            if (nextSession < 0) throw new IllegalArgumentException("Invalid session");
            String nextName = message.get("name").getAsString();
            if (nextName.length() > 64) throw new IllegalArgumentException("Name too long");
            String nextPanel=message.has("panel")?message.get("panel").getAsString():"";
            if(nextPanel.length()>256)throw new IllegalArgumentException("Panel name too long");
            String nextTitle = title(message, null);
            String inventory=null,crafting=null;
            if(message.has("labels")) {
                var labels=message.getAsJsonObject("labels");if(labels.size()>2)throw new IllegalArgumentException("Menu labels");
                for(var entry:labels.entrySet()){if(!Set.of("inventory","crafting").contains(entry.getKey()))throw new IllegalArgumentException("Menu label");ProtocolText.validate(entry.getValue(),256);}
                if(labels.has("inventory"))inventory=labels.get("inventory").getAsString();
                if(labels.has("crafting"))crafting=labels.get("crafting").getAsString();
            }
            float nextYaw = finite(message.get("yaw").getAsFloat());
            SharingRules.Side nextSide=message.has("side")?SharingRules.Side.valueOf(message.get("side").getAsString()):SharingRules.Side.BOTH;
            int nextDistance=message.has("distance")?dev.wigo.config.SharingDistance.validate(message.get("distance").getAsInt()):dev.wigo.config.SharingDistance.DEFAULT;
            int width = message.has("width") ? message.get("width").getAsInt() : 176;
            int height = message.has("height") ? message.get("height").getAsInt() : 166;
            if (width < 1 || width > 512 || height < 1 || height > 384) throw new IllegalArgumentException("Panel size");
            long initialRevision = message.has("revision") ? message.get("revision").getAsLong() : 0;
            if (initialRevision < 0) throw new IllegalArgumentException("Invalid revision");
            kind = nextKind; rows = nextRows; slotCount = count;
            inventoryCaption=inventory;craftingCaption=crafting;
            guiWidth = width; guiHeight = height;
            name = nextName; yaw = nextYaw; session = nextSession;
            side=nextSide;
            distance=nextDistance;
            panel=nextPanel;
            title = nextTitle;
            revision = initialRevision; pendingRevision = -1;
            slots = new JsonElement[count]; Arrays.fill(slots, JsonNull.INSTANCE);
            pending = null; carried = JsonNull.INSTANCE; ready = false;
            mouseX = mouseY = -16; hovered = -1;
            cursorStyle=CursorStyle.ARROW;
            return false;
        }
        if (slots == null || message.get("session").getAsLong() != session) return false;
        if(op.equals("side")){side=SharingRules.Side.valueOf(message.get("side").getAsString());return false;}
        if(op.equals("distance")){distance=dev.wigo.config.SharingDistance.validate(message.get("distance").getAsInt());return false;}
        if (op.equals("cursor")) {
            float x = finite(message.get("x").getAsFloat());
            float y = finite(message.get("y").getAsFloat());
            int slot = message.get("hover").getAsInt();
            CursorStyle style=message.has("cursorStyle")?CursorStyle.fromId(message.get("cursorStyle").getAsInt()):cursorStyle;
            mouseX = dev.wigo.protocol.LegacyClamp.clamp(x, -16, width() + 16);
            mouseY = dev.wigo.protocol.LegacyClamp.clamp(y, -16, height() + 16);
            hovered = slot >= 0 && slot < slotCount ? slot : -1;
            cursorStyle=style;
            return false;
        }
        long rev = message.get("revision").getAsLong();
        if (rev != revision + 1) return false;
        if(op.equals("update")) {
            JsonObject changes=message.getAsJsonObject("slots");if(changes.size()>slotCount)throw new IllegalArgumentException("Too many slots");
            for(var entry:changes.entrySet()) {
                int index=Integer.parseInt(entry.getKey());if(index<0||index>=slotCount)throw new IllegalArgumentException("Invalid slot");validateStack(entry.getValue());
            }
            JsonElement nextCarried=message.has("carried")?message.get("carried"):carried;validateStack(nextCarried);
            String nextTitle=title(message,title);JsonElement[] next=slots.clone();
            for(var entry:changes.entrySet())next[Integer.parseInt(entry.getKey())]=dev.wigo.protocol.LegacyJson.copy(entry.getValue());
            slots=next;carried=dev.wigo.protocol.LegacyJson.copy(nextCarried);title=nextTitle;pending=null;pendingRevision=-1;revision=rev;ready=true;return true;
        }
        if (op.equals("patch")) {
            JsonObject changes = message.getAsJsonObject("slots");
            if (changes.size() > slotCount) throw new IllegalArgumentException("Too many slots");
            // Validate everything before modifying the pending revision.
            for (var entry : changes.entrySet()) {
                int index = Integer.parseInt(entry.getKey());
                if (index < 0 || index >= slotCount) throw new IllegalArgumentException("Invalid slot");
                validateStack(entry.getValue());
            }
            if (pending == null) { pending = slots.clone(); pendingRevision = rev; }
            if (pendingRevision != rev) return false;
            for (var entry : changes.entrySet()) pending[Integer.parseInt(entry.getKey())] = dev.wigo.protocol.LegacyJson.copy(entry.getValue());
            return false;
        }
        if (op.equals("commit")) {
            JsonElement nextCarried = message.has("carried") ? message.get("carried") : carried;
            validateStack(nextCarried);
            String nextTitle = title(message, title);
            if (pending != null) slots = pending;
            carried = dev.wigo.protocol.LegacyJson.copy(nextCarried); pending = null; pendingRevision = -1;
            title = nextTitle;
            revision = rev; ready = true;
            return true;
        }
        throw new IllegalArgumentException("Unknown operation");
    }

    private static void validateStack(JsonElement stack) {
        if (!stack.isJsonNull() && (!stack.isJsonObject() || stack.toString().length() > MAX_STACK_CHARS))
            throw new IllegalArgumentException("Invalid item description");
    }
    private static String title(JsonObject message, String fallback) {
        if (!message.has("title")) return fallback;
        var value = message.get("title");
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || value.getAsString().length() > MAX_TITLE_CHARS) throw new IllegalArgumentException("Invalid menu title");
        return value.getAsString();
    }
    private static float finite(float value) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException("Invalid coordinate");
        return value;
    }
    public int width() { return kind.equals("gui") ? guiWidth : 176; }
    public int height() { return kind.equals("chest") ? 114 + rows * 18 : kind.equals("unsupported") ? 64 : kind.equals("gui") ? guiHeight : 166; }
    public JsonElement slot(int index) { return slots[index]; }
    public JsonElement carried() { return carried; }
}
