package dev.wigo.protocol;

import com.google.gson.*;
import java.util.*;

/** Bounded display instructions, with one current scene and one atomic pending revision. */
public final class GuiScene {
    public static final int MAX_COMMANDS = 2048, MAX_SCENE_CHARS = 196608, MAX_COMMAND_CHARS = 8192;
    public long session, revision;
    public boolean ready;
    public String title = "";
    private JsonElement titleValue=new JsonPrimitive("");
    public float left, top, rawWidth = 176, rawHeight = 166;
    private JsonArray[] commands = new JsonArray[0], pending;
    private JsonObject pendingHeader;
    private JsonElement carried = JsonNull.INSTANCE;

    public GuiScene(long session, long revision) { this.session = session; this.revision = revision; }
    public boolean accept(JsonObject packet) {
        if (packet.get("session").getAsLong() != session) return false;
        long rev = packet.get("revision").getAsLong();
        if (rev != revision + 1) return false;
        String op = packet.get("op").getAsString();
        if(op.equals("ui_update"))return atomic(packet);
        if (op.equals("ui_begin")) {
            int count = packet.get("count").getAsInt();
            if (count < 0 || count > MAX_COMMANDS) throw new IllegalArgumentException("Scene command limit");
            JsonArray rect = packet.getAsJsonArray("rect");
            if (rect.size() != 4) throw new IllegalArgumentException("Scene bounds");
            for (JsonElement v : rect) finite(v.getAsFloat());
            if (rect.get(2).getAsFloat() < 1 || rect.get(3).getAsFloat() < 1
                    || rect.get(2).getAsFloat() > 8192 || rect.get(3).getAsFloat() > 8192)
                throw new IllegalArgumentException("Scene dimensions");
            ProtocolText.validate(packet.get("title"),256);
            pendingHeader = dev.wigo.protocol.LegacyJson.copy(packet);
            pending = packet.has("full") && packet.get("full").getAsBoolean() ? new JsonArray[count] : Arrays.copyOf(commands, count);
            return false;
        }
        if (pending == null) return false;
        if (op.equals("ui_patch")) {
            JsonObject changes = packet.getAsJsonObject("changes");
            if (changes.size() > MAX_COMMANDS) throw new IllegalArgumentException("Scene patch limit");
            for (var entry : changes.entrySet()) {
                int index = Integer.parseInt(entry.getKey());
                if (index < 0 || index >= pending.length) throw new IllegalArgumentException("Scene index");
                validateCommand(entry.getValue().getAsJsonArray());
            }
            JsonArray copies=packet.has("copies")?packet.getAsJsonArray("copies"):new JsonArray();
            if(copies.size()>MAX_COMMANDS||copies.size()!=0&&pendingHeader.get("full").getAsBoolean())throw new IllegalArgumentException("Scene copy limit");
            BitSet used=new BitSet(pending.length);int copiedChars=0;
            for(var value:copies) {
                var range=value.getAsJsonArray();if(range.size()!=3)throw new IllegalArgumentException("Scene copy shape");
                for(var number:range)if(!number.isJsonPrimitive()||!number.getAsJsonPrimitive().isNumber()||!number.toString().matches("[0-9]{1,4}"))throw new IllegalArgumentException("Scene copy index");
                int from=range.get(0).getAsInt(),to=range.get(1).getAsInt(),length=range.get(2).getAsInt();
                if(length<1||from>commands.length-length||to>pending.length-length)throw new IllegalArgumentException("Scene copy bounds");
                for(int index=0;index<length;index++) {
                    if(used.get(to+index))throw new IllegalArgumentException("Overlapping scene copy");used.set(to+index);
                    copiedChars+=commands[from+index].toString().length();if(copiedChars>MAX_SCENE_CHARS)throw new IllegalArgumentException("Scene copy budget");
                }
            }
            for(var value:copies) {
                var range=value.getAsJsonArray();int from=range.get(0).getAsInt(),to=range.get(1).getAsInt(),length=range.get(2).getAsInt();
                for(int index=0;index<length;index++)pending[to+index]=dev.wigo.protocol.LegacyJson.copy(commands[from+index]);
            }
            for (var entry : changes.entrySet()) pending[Integer.parseInt(entry.getKey())] = dev.wigo.protocol.LegacyJson.copy(entry.getValue().getAsJsonArray());
            return false;
        }
        if (!op.equals("ui_commit")) throw new IllegalArgumentException("Unknown scene operation");
        int chars = 0;
        for (JsonArray command : pending) {
            if (command == null) throw new IllegalArgumentException("Incomplete scene");
            chars += command.toString().length();
        }
        if (chars > MAX_SCENE_CHARS) throw new IllegalArgumentException("Scene byte budget");
        JsonElement nextCarried = packet.has("carried") ? packet.get("carried") : carried;
        if (!nextCarried.isJsonNull() && (!nextCarried.isJsonObject() || nextCarried.toString().length() > MirrorState.MAX_STACK_CHARS))
            throw new IllegalArgumentException("Scene carried item");
        JsonArray rect = pendingHeader.getAsJsonArray("rect");
        left = rect.get(0).getAsFloat(); top = rect.get(1).getAsFloat();
        rawWidth = rect.get(2).getAsFloat(); rawHeight = rect.get(3).getAsFloat();
        titleValue=dev.wigo.protocol.LegacyJson.copy(pendingHeader.get("title"));title=titleValue.isJsonPrimitive()?titleValue.getAsString():"";
        commands = pending; pending = null; pendingHeader = null;
        carried = dev.wigo.protocol.LegacyJson.copy(nextCarried); revision = rev; ready = true;
        return true;
    }
    private boolean atomic(JsonObject update) {
        GuiScene next=new GuiScene(session,revision);next.commands=commands;next.carried=carried;
        JsonObject begin=dev.wigo.protocol.LegacyJson.copy(update);begin.addProperty("op","ui_begin");next.accept(begin);
        JsonObject patch=packet("ui_patch",session,revision+1);patch.add("changes",update.get("changes"));if(update.has("copies"))patch.add("copies",update.get("copies"));next.accept(patch);
        JsonObject commit=packet("ui_commit",session,revision+1);if(update.has("carried"))commit.add("carried",update.get("carried"));
        if(!next.accept(commit))return false;
        commands=next.commands;carried=next.carried;title=next.title;titleValue=next.titleValue;
        left=next.left;top=next.top;rawWidth=next.rawWidth;rawHeight=next.rawHeight;
        revision=next.revision;ready=true;pending=null;pendingHeader=null;return true;
    }
    public static boolean isWigoIcon(String id) {
        return switch(id) {
            case "wigo:textures/gui/face.png","wigo:textures/gui/modrinth.png","wigo:textures/gui/curseforge.png","wigo:textures/gui/info.png","wigo:textures/gui/github.png","wigo:textures/gui/issues.png",
                    "wigo:textures/gui/cursor/food.png","wigo:textures/gui/cursor/air.png","wigo:textures/gui/cursor/search.png",
                    "wigo:textures/gui/cursor/withered.png","wigo:textures/gui/cursor/heart.png","wigo:textures/gui/cursor/bug.png" -> true;
            default -> false;
        };
    }
    public static void validateCommand(JsonArray c) {
        if (c.size() < 4 || c.toString().length() > MAX_COMMAND_CHARS) throw new IllegalArgumentException("Scene command");
        String type = c.get(0).getAsString();
        int length=switch(type) {case "r","t","e","c"->9;case "b","s"->13;case "a"->14;case "i","o"->7;case "k","v","h"->8;case "n"->6;case "m","l"->5;case "z","q"->4;default->throw new IllegalArgumentException("Unknown drawing instruction");};
        if (c.size()!=length && !(type.equals("t")&&(c.size()==10||c.size()==11))) throw new IllegalArgumentException("Drawing instruction arguments");
        if (!c.get(1).isJsonNull()) {
            JsonArray matrix = c.get(1).getAsJsonArray();
            if (matrix.size() != 6) throw new IllegalArgumentException("Scene transform");
            for (JsonElement v : matrix) finite(v.getAsFloat());
        }
        if (!c.get(2).isJsonNull()) {
            JsonArray scissor = c.get(2).getAsJsonArray();
            if (scissor.size() != 4) throw new IllegalArgumentException("Scene clip");
            for (JsonElement v : scissor) finite(v.getAsFloat());
        }
        // Local vanilla resources and packaged F8 icons. URLs/source pixels are not instructions.
        if (Set.of("b", "s", "a", "k", "l").contains(type)
                && !(type.equals("b")&&isWigoIcon(c.get(3).getAsString()))) resource(c.get(3).getAsString());
        if(type.equals("l")) {
            if(!c.get(3).getAsString().matches("minecraft:textures/entity/signs/[a-z_]+\\.png"))throw new IllegalArgumentException("Sign texture");
            c.get(4).getAsBoolean();
        }
        if(type.equals("o")) {
            resource(c.get(3).getAsString());
            if(!c.get(3).getAsString().matches("minecraft:[a-z_]+"))throw new IllegalArgumentException("Sign wood type");
            rect(c.get(4));c.get(6).getAsBoolean();
        }
        if (type.equals("a")) resource(c.get(4).getAsString());
        if(type.equals("t"))ProtocolText.validate(c.get(3),2048);
        if(type.equals("t")&&c.size()>=10&&!c.get(9).isJsonNull()) {
            JsonArray runs=c.get(9).getAsJsonArray();if(runs.size()>128)throw new IllegalArgumentException("Text style limit");
            StringBuilder plain=new StringBuilder();boolean literal=c.get(3).isJsonPrimitive();
            for(var value:runs) {
                JsonArray run=value.getAsJsonArray();if(run.size()!=4)throw new IllegalArgumentException("Text style");
                ProtocolText.validate(run.get(0),2048);if(run.get(0).isJsonPrimitive())plain.append(run.get(0).getAsString());else literal=false;resource(run.get(1).getAsString());
                if(run.get(3).getAsInt()<0||run.get(3).getAsInt()>31)throw new IllegalArgumentException("Text style flags");
                if(!run.get(2).isJsonNull())run.get(2).getAsInt();
            }
            if(literal&&!plain.toString().equals(c.get(3).getAsString()))throw new IllegalArgumentException("Text style content");
        }
        if(type.equals("t")&&c.size()==11){
            var placement=c.get(10).getAsJsonArray();if(placement.size()!=3||placement.get(0).getAsInt()!=0)throw new IllegalArgumentException("Text placement");
            finite(placement.get(1).getAsFloat());int width=placement.get(2).getAsInt();if(width<1||width>8192)throw new IllegalArgumentException("Text placement width");
        }
        if (type.equals("i") && (!c.get(3).isJsonObject() || c.get(3).toString().length() > MirrorState.MAX_STACK_CHARS))
            throw new IllegalArgumentException("Scene item");
        if (type.equals("q") && c.get(3).getAsString().length()>256) throw new IllegalArgumentException("Chat draft length");
        if (type.equals("m")) {
            int y=c.get(3).getAsInt();if (y<0||y>96||y%32!=0) throw new IllegalArgumentException("Map strip bounds");
            MapPalette.decode(c.get(4).getAsString());
        }
        if (type.equals("e")) {
            if (!c.get(4).isJsonObject()||c.get(4).toString().length()>8192) throw new IllegalArgumentException("Entity preview");
            JsonObject preview=c.get(4).getAsJsonObject();
            if (preview.has("type")) {resource(preview.get("type").getAsString());if (preview.getAsJsonObject("equipment").size()>8) throw new IllegalArgumentException("Entity equipment");}
            if(preview.has("angles"))vector(preview.get("angles"),4);
            if(preview.has("stand")) {
                var flags=preview.getAsJsonArray("stand");if(flags.size()!=2)throw new IllegalArgumentException("Armor stand flags");
                for(var flag:flags)if(!flag.isJsonPrimitive()||!flag.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("Armor stand flag");
            }
            if(preview.has("transform")) {
                var transform=preview.getAsJsonArray("transform");if(transform.size()!=3)throw new IllegalArgumentException("Entity transform");
                vector(transform.get(0),3);vector(transform.get(1),4);if(!transform.get(2).isJsonNull())vector(transform.get(2),4);
            }
        }
        if (type.equals("e")||type.equals("n")) rect(c.get(5));
        if (type.equals("k")) rect(c.get(4));
        if (type.equals("h")) {
            rect(c.get(4));
            if (c.get(6).getAsJsonArray().size()!=3||c.get(7).getAsJsonArray().size()!=4||!c.get(3).isJsonNull()&&!c.get(3).isJsonObject()) throw new IllegalArgumentException("Smithing preview");
        }
        if (type.equals("v")) rect(c.get(3));
        if (type.equals("t")) c.get(7).getAsBoolean();
        if(type.equals("c")) {
            for(int i=1;i<8;i++)finiteNumbers(c.get(i));
            c.get(6).getAsBoolean();c.get(7).getAsBoolean();long epoch=c.get(8).getAsLong();
            if(epoch<0||epoch>4102444800000L)throw new IllegalArgumentException("Caret clock");
        }else finiteNumbers(c);
    }
    private static void rect(JsonElement value) {if (!value.isJsonArray()||value.getAsJsonArray().size()!=4) throw new IllegalArgumentException("Preview bounds");}
    private static void vector(JsonElement value,int length) {
        if(!value.isJsonArray()||value.getAsJsonArray().size()!=length)throw new IllegalArgumentException("Preview vector");
        for(var number:value.getAsJsonArray()) {
            if(!number.isJsonPrimitive()||!number.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Preview number");
            finite(number.getAsFloat());
        }
    }
    private static void finiteNumbers(JsonElement value) {
        if (value.isJsonArray()) for (JsonElement child : value.getAsJsonArray()) finiteNumbers(child);
        else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) finite(value.getAsFloat());
    }
    private static void finite(float v) { if (!Float.isFinite(v) || Math.abs(v) > 1e10f) throw new IllegalArgumentException("Scene number"); }
    public static void resource(String id) {
        if (!id.startsWith("minecraft:") || id.length() > 256 || !id.substring(10).matches("[a-z0-9/._-]+"))
            throw new IllegalArgumentException("Non-vanilla resource");
    }
    public JsonArray[] commands() { return commands; }
    public JsonElement carried() { return carried; }
    public JsonElement titleValue(){return titleValue;}
    public float scale() { return Math.min(1, Math.min(512 / rawWidth, 384 / rawHeight)); }
    public int width() { return Math.max(1, (int)Math.ceil(rawWidth * scale())); }
    public int height() { return Math.max(1, (int)Math.ceil(rawHeight * scale())); }
    public static List<JsonObject> messages(long session, long revision, String title, JsonArray rect,
                                            JsonArray[] next, JsonArray[] previous, JsonElement carried, boolean full) {
        return messages(session,revision,new JsonPrimitive(title),rect,next,previous,carried,full,false);
    }
    public static List<JsonObject> messages(long session,long revision,JsonElement title,JsonArray rect,
                                            JsonArray[] next,JsonArray[] previous,JsonElement carried,boolean full,boolean atomic) {
        return messages(session,revision,title,rect,next,previous,carried,full,atomic,false);
    }
    public static List<JsonObject> messages(long session,long revision,JsonElement title,JsonArray rect,
                                            JsonArray[] next,JsonArray[] previous,JsonElement carried,boolean full,boolean atomic,boolean copyRanges) {
        if(copyRanges&&!full&&next.length!=previous.length) {
            int suffix=0;while(suffix<Math.min(next.length,previous.length)&&Objects.equals(next[next.length-1-suffix],previous[previous.length-1-suffix]))suffix++;
            if(suffix>=3) {
                var ordinary=messagesInternal(session,revision,title,rect,next,previous,carried,full,atomic,0);
                var copied=messagesInternal(session,revision,title,rect,next,previous,carried,full,atomic,suffix);
                if(copied.stream().mapToInt(p->p.toString().length()).sum()<ordinary.stream().mapToInt(p->p.toString().length()).sum())return copied;
                return ordinary;
            }
        }
        return messagesInternal(session,revision,title,rect,next,previous,carried,full,atomic,0);
    }
    private static List<JsonObject> messagesInternal(long session,long revision,JsonElement title,JsonArray rect,
                                            JsonArray[] next,JsonArray[] previous,JsonElement carried,boolean full,boolean atomic,int suffix) {
        List<JsonObject> result = new ArrayList<>();
        JsonObject begin = packet("ui_begin", session, revision);
        begin.addProperty("count", next.length); begin.add("title", title);
        begin.addProperty("full", full); begin.add("rect", dev.wigo.protocol.LegacyJson.copy(rect)); result.add(begin);
        JsonObject changes = new JsonObject(); int size = 0;
        for (int i = 0; i < next.length; i++) {
            if(suffix>0&&i>=next.length-suffix)continue;
            if (!full && i < previous.length && Objects.equals(next[i], previous[i])) continue;
            int length = next[i].toString().length() + 12;
            if (size + length > 12000 && changes.size()!=0) {
                JsonObject patch = packet("ui_patch", session, revision); patch.add("changes", changes); result.add(patch);
                changes = new JsonObject(); size = 0;
            }
            changes.add(Integer.toString(i), next[i]); size += length;
        }
        JsonArray copies=new JsonArray();
        if(suffix>0){JsonArray range=new JsonArray();range.add(previous.length-suffix);range.add(next.length-suffix);range.add(suffix);copies.add(range);}
        if (changes.size()!=0||suffix>0) { JsonObject patch = packet("ui_patch", session, revision); patch.add("changes", changes);if(suffix>0)patch.add("copies",copies);result.add(patch); }
        JsonObject commit = packet("ui_commit", session, revision); commit.add("carried", carried); result.add(commit);
        if(atomic&&result.size()<=3) {
            JsonObject update=dev.wigo.protocol.LegacyJson.copy(begin);update.addProperty("op","ui_update");update.add("changes",changes);update.add("carried",carried);
            if(suffix>0)update.add("copies",copies);
            if(update.toString().length()<=Wire.MAX_TEXT-256)return List.of(update);
        }
        return result;
    }
    public JsonArray rect() { JsonArray a = new JsonArray(); a.add(left); a.add(top); a.add(rawWidth); a.add(rawHeight); return a; }
    private static JsonObject packet(String op, long session, long revision) {
        JsonObject packet = new JsonObject(); packet.addProperty("op", op); packet.addProperty("session", session); packet.addProperty("revision", revision); return packet;
    }
}
