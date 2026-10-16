package dev.wigo.protocol;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;

/** Checks partial delivery, stale sessions, observers joining late, and hostile bounds. */
public final class MirrorStateTest {
    private static int checks;
    public static void main(String[] args) {
        MirrorState a = new MirrorState();
        a.accept(open(1, 0));
        a.accept(json("{op:patch,session:1,revision:1,slots:{0:{id:'minecraft:diamond',count:12}}}"));
        check(!a.ready && a.slot(0).isJsonNull(), "Partial snapshot must not be visible");
        a.accept(json("{op:patch,session:1,revision:1,slots:{5:{id:'minecraft:oak_log',count:32}}}"));
        check(a.accept(json("{op:commit,session:1,revision:1,carried:null}")), "Snapshot commits");
        check(a.slot(0).getAsJsonObject().get("count").getAsInt() == 12, "Snapshot item count");
        a.accept(json("{op:patch,session:1,revision:2,slots:{0:null}}"));
        check(!a.slot(0).isJsonNull(), "Source slot remains until transaction commits");
        a.accept(json("{op:patch,session:1,revision:2,slots:{8:{id:'minecraft:diamond',count:12}}}"));
        a.accept(json("{op:commit,session:1,revision:2}"));
        check(a.slot(0).isJsonNull() && !a.slot(8).isJsonNull(), "Move applies atomically");
        check(!a.accept(json("{op:commit,session:1,revision:1}")) && a.revision == 2, "Old revision ignored");
        a.accept(open(2, 0));
        check(!a.ready && a.slot(8).isJsonNull(), "New menu clears previous contents");
        check(!a.accept(json("{op:patch,session:1,revision:1,slots:{8:{id:'minecraft:stone',count:64}}}")), "Old session ignored");
        rejects(() -> a.accept(json("{op:patch,session:2,revision:1,slots:{0:null,90:null}}")));
        a.accept(json("{op:commit,session:2,revision:1}"));
        check(a.slot(0).isJsonNull(), "Rejected patch has no side effects");
        rejects(() -> a.accept(json("{op:cursor,session:2,x:NaN,y:4,hover:0}")));
        rejects(() -> a.accept(json("{op:open,session:3,kind:'chest',rows:900,size:8136,name:'Actor',yaw:0}")));
        MirrorState late = new MirrorState();
        late.accept(open(2, 100));
        late.accept(json("{op:patch,session:2,revision:101,slots:{8:{id:'minecraft:diamond',count:12}}}"));
        late.accept(json("{op:commit,session:2,revision:101,carried:null}"));
        check(late.ready && late.revision == 101 && !late.slot(8).isJsonNull(), "Late observer accepts current revision");
        late.accept(json("{op:patch,session:2,revision:102,slots:{8:null}}"));
        late.accept(json("{op:commit,session:2,revision:102}"));
        check(late.slot(8).isJsonNull(), "Late observer follows later updates");
        String countUpdate = "{\"op\":\"patch\",\"session\":42,\"revision\":7,\"slots\":{\"5\":{\"id\":\"minecraft:diamond\",\"count\":32}}}";
        check(countUpdate.getBytes(StandardCharsets.UTF_8).length < 128, "One ordinary slot update fits in 128 JSON bytes");
        MirrorState named=new MirrorState();
        named.accept(json("{op:open,session:8,kind:unsupported,rows:0,size:0,name:Actor,yaw:0,panel:'Настройки графики'}"));
        check(named.panel.equals("Настройки графики"),"Unsupported menu preserves its panel name");
        named.accept(open(9,0));check(named.panel.isEmpty(),"Next menu clears the previous panel name");
        JsonObject oversized=open(10,0);oversized.addProperty("panel","x".repeat(257));
        rejects(()->named.accept(oversized));check(named.session==9,"Invalid panel name has no side effects");
        JsonObject titled = open(20, 0); titled.addProperty("title", "Ресурсы \"друга\" 📦");
        named.accept(titled);
        check(named.title.equals("Ресурсы \"друга\" 📦"), "Storage title preserves Unicode and quotes");
        named.accept(json("{op:commit,session:20,revision:1}"));
        named.accept(json("{op:cursor,session:20,x:8,y:8,hover:0}"));
        check(named.title.equals("Ресурсы \"друга\" 📦"), "Cursor and slot commits preserve the title");
        named.accept(json("{op:patch,session:20,revision:2,slots:{0:{id:'minecraft:stone',count:3}}}"));
        JsonObject invalidTitle = json("{op:commit,session:20,revision:2}");
        invalidTitle.addProperty("title", "x".repeat(257));
        rejects(() -> named.accept(invalidTitle));
        check(named.revision == 1 && named.slot(0).isJsonNull() && named.title.equals("Ресурсы \"друга\" 📦"),
                "Invalid title cannot partially commit slots or metadata");
        named.accept(json("{op:commit,session:20,revision:2,title:'Новый шалкер'}"));
        check(named.title.equals("Новый шалкер") && !named.slot(0).isJsonNull(), "Title updates with the atomic revision");
        check(!named.accept(json("{op:commit,session:19,revision:3,title:'Old'}")) && named.title.equals("Новый шалкер"),
                "Stale session cannot replace a storage title");
        for (JsonElement value : new JsonElement[]{JsonNull.INSTANCE, new JsonPrimitive(7), new JsonArray()}) {
            JsonObject invalid = open(21, 0); invalid.add("title", value);
            rejects(() -> named.accept(invalid));
            check(named.session == 20, "Invalid title type has no open side effects");
        }
        JsonObject emptyTitle = open(21, 100); emptyTitle.addProperty("title", "");
        named.accept(emptyTitle);
        check(named.title.isEmpty(), "Explicit empty title is distinct from a missing title");
        named.accept(open(22, 0)); check(named.title == null, "Legacy snapshot clears the previous storage title");
        check(named.side==dev.wigo.config.SharingRules.Side.BOTH,"Legacy menu without side metadata must remain two-sided");
        JsonObject oneSided=open(23,0);oneSided.addProperty("side","BACK");named.accept(oneSided);
        named.accept(json("{op:update,session:23,revision:1,slots:{0:{id:'minecraft:diamond',count:64}}}"));
        check(named.side==dev.wigo.config.SharingRules.Side.BACK&&named.ready,"Side metadata missing from ready snapshot");
        named.accept(json("{op:side,session:23,side:FRONT}"));
        check(named.side==dev.wigo.config.SharingRules.Side.FRONT&&named.ready&&named.revision==1&&named.slot(0).getAsJsonObject().get("count").getAsInt()==64,"Changing sides resets the ready menu/contents");
        named.accept(json("{op:side,session:22,side:BACK}"));check(named.side==dev.wigo.config.SharingRules.Side.FRONT,"Old side update changes the current panel");
        rejects(()->named.accept(json("{op:side,session:23,side:AROUND}")));
        check(named.side==dev.wigo.config.SharingRules.Side.FRONT&&named.ready,"Invalid side update has side effects");
        JsonObject badSide=open(24,0);badSide.addProperty("side","AROUND");rejects(()->named.accept(badSide));
        check(named.session==23&&named.side==dev.wigo.config.SharingRules.Side.FRONT&&named.ready,"Invalid opening side replaces a valid mirror");
        for(var style:dev.wigo.config.CursorStyle.values()) {
            named.accept(json("{op:cursor,session:23,x:88,y:83,hover:-1,cursorStyle:"+style.id+"}"));
            check(named.cursorStyle==style&&named.ready&&named.revision==1,"Cursor choice changes inventory state");
            named.accept(json("{op:cursor,session:23,x:89,y:84,hover:0}"));
            check(named.cursorStyle==style,"Sparse pointer update forgets the selected cursor");
        }
        named.accept(json("{op:cursor,session:22,x:0,y:0,hover:0,cursorStyle:1}"));
        check(named.cursorStyle==dev.wigo.config.CursorStyle.BUG,"Old pointer session changes cursor choice");
        named.accept(json("{op:cursor,session:23,x:88,y:83,hover:-1,cursorStyle:1000000}"));
        check(named.cursorStyle==dev.wigo.config.CursorStyle.ARROW,"Unknown cursor must use normal arrow");
        named.accept(json("{op:cursor,session:23,x:88,y:83,hover:-1,cursorStyle:5}"));
        named.accept(open(24,0));
        check(named.cursorStyle==dev.wigo.config.CursorStyle.ARROW,"New menu inherits previous cursor choice");
        System.out.println("WIGO protocol: " + checks + " checks passed; example slot update: " + countUpdate.length() + " JSON bytes");
        GuiSceneTest.run();
        CompressedTextTest.run();
        CompactCodecTest.run();
        PacketTemplatesTest.run();
        NetworkBatchTest.run();
        ShortTextTest.run();RichTextTest.run();
        dev.wigo.config.MenuConfigTest.run();
        dev.wigo.config.SharingRulesTest.run();
        dev.wigo.client.PanelShapeTest.run();
        dev.wigo.client.PanelLightingTest.run();
        dev.wigo.client.UpdateCheckerTest.run();
        dev.wigo.client.ObserverIndicatorTest.run();
    }
    private static JsonObject open(long session, long revision) {
        return json("{op:open,session:" + session + ",revision:" + revision + ",kind:'chest',rows:3,size:63,name:'Actor',yaw:0}");
    }
    private static JsonObject json(String source) { return new com.google.gson.JsonParser().parse(source).getAsJsonObject(); }
    private static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    private static void rejects(Runnable operation) {
        try { operation.run(); throw new AssertionError("Invalid packet accepted"); }
        catch (IllegalArgumentException | UnsupportedOperationException expected) { checks++; }
    }
}
