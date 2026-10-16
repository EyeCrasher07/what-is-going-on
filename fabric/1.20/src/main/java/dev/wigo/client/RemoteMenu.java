package dev.wigo.client;

import com.google.gson.*;
import dev.wigo.StackJson;
import dev.wigo.protocol.MirrorState;
import dev.wigo.protocol.GuiScene;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class RemoteMenu {
    public final UUID owner;
    public final MirrorState state = new MirrorState();
    public GuiScene scene;
    public boolean renderUnsupported;
    public ItemStack[] items = new ItemStack[0];
    private JsonElement[] decoded = new JsonElement[0];
    public ItemStack carried = ItemStack.EMPTY;
    private JsonElement decodedCarried = JsonNull.INSTANCE;
    private float startX = -16, startY = -16, targetX = -16, targetY = -16;
    private long cursorTime;
    private long readyTime;
    private final Map<Integer,ItemStack> dragItems=new HashMap<>();
    private ItemStack dragCarried;

    public RemoteMenu(UUID owner) { this.owner = owner; }
    public boolean isChatPreview() {
        if(scene==null||!scene.ready)return false;
        for(var command:scene.commands())if(command.get(0).getAsString().equals("q"))return true;
        return false;
    }
    public void accept(JsonObject json, RegistryAccess registries) {
        long now = System.nanoTime();
        String op = json.get("op").getAsString();
        if (!op.equals("open") && json.get("session").getAsLong() != state.session) return;
        if(op.equals("drag")) {
            if(!state.ready||json.get("revision").getAsLong()!=state.revision)return;
            var next=new HashMap<Integer,ItemStack>();var changes=json.getAsJsonArray("slots");
            if(changes.size()>items.length)throw new IllegalArgumentException("Drag preview size");
            for(var entry:changes) {
                var pair=entry.getAsJsonArray();if(pair.size()!=2)throw new IllegalArgumentException("Drag preview entry");
                int index=pair.get(0).getAsInt(),count=pair.get(1).getAsInt();
                if(index<0||index>=items.length||count<0||count>carried.getMaxStackSize()||next.containsKey(index))throw new IllegalArgumentException("Drag preview count");
                next.put(index,carried.copyWithCount(count));
            }
            int count=json.get("carriedCount").getAsInt();
            if(count<0||count>carried.getMaxStackSize())throw new IllegalArgumentException("Drag cursor count");
            dragItems.clear();dragItems.putAll(next);dragCarried=changes.size()==0?null:carried.copyWithCount(count);return;
        }
        if (op.startsWith("ui_")) {
            if (scene != null && scene.accept(json)) {
                state.guiWidth = scene.width(); state.guiHeight = scene.height(); state.ready = true;
                if(readyTime==0)readyTime=now;
                carried = StackJson.decode(scene.carried(), registries);
            }
            return;
        }
        if (op.equals("cursor")) {
            startX = mouseX(now); startY = mouseY(now);
        }
        boolean committed = state.accept(json);
        if (op.equals("open")) {
            readyTime=0;
            dragItems.clear();dragCarried=null;
            scene = state.kind.equals("gui") ? new GuiScene(state.session, json.has("uiRevision") ? json.get("uiRevision").getAsLong() : 0) : null;
            items = new ItemStack[state.slotCount]; Arrays.fill(items, ItemStack.EMPTY);
            decoded = new JsonElement[state.slotCount];
            decodedCarried = JsonNull.INSTANCE; carried = ItemStack.EMPTY;
            startX = startY = targetX = targetY = -16; cursorTime = 0;
        }
        if (op.equals("cursor")) {
            targetX = state.mouseX; targetY = state.mouseY;
            if (cursorTime == 0) { startX = targetX; startY = targetY; }
            cursorTime = now;
        }
        if (!committed) return;
        if(readyTime==0)readyTime=now;
        dragItems.clear();dragCarried=null;
        for (int i = 0; i < items.length; i++) {
            JsonElement value = state.slot(i);
            if (!Objects.equals(value, decoded[i])) {
                items[i] = StackJson.decode(value, registries); decoded[i] = value;
            }
        }
        if (!Objects.equals(decodedCarried, state.carried())) {
            carried = StackJson.decode(state.carried(), registries); decodedCarried = state.carried();
        }
    }
    public ItemStack displayItem(int slot) {return dragItems.getOrDefault(slot,items[slot]);}
    public ItemStack displayCarried() {return dragCarried==null?carried:dragCarried;}
    public float mouseX(long now) { return startX + (targetX - startX) * fraction(now); }
    public float mouseY(long now) { return startY + (targetY - startY) * fraction(now); }
    public double openingOpacity(long now) {
        if(readyTime==0)return 0;
        double fraction=dev.wigo.protocol.LegacyClamp.clamp((now-readyTime)/180_000_000.0,0,1);
        return fraction*fraction*(3-2*fraction);
    }
    private float fraction(long now) { return dev.wigo.protocol.LegacyClamp.clamp((now - cursorTime) / 40_000_000f, 0, 1); }
}
