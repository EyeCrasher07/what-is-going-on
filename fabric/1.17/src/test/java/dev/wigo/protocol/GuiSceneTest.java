package dev.wigo.protocol;

import com.google.gson.*;
import java.util.*;

public final class GuiSceneTest {
    private static int checks;
    public static void run() {
        copies();
        entityTransform();
        GuiScene scene=new GuiScene(5,0);
        JsonArray rect=a(0,0,176,166);
        JsonArray[] initial={a("r",null,null,0,0,176,166,-1,-1),a("t",null,null,"Chest",8,6,-1,false,0)};
        var full=GuiScene.messages(5,1,"Chest",rect,initial,new JsonArray[0],JsonNull.INSTANCE,true);
        scene.accept(full.get(0));scene.accept(full.get(1));
        check(!scene.ready,"Partial GUI is invisible");
        check(scene.accept(full.get(2))&&scene.commands().length==2,"Atomic full GUI");
        JsonArray[] moved={initial[0],a("t",null,null,"Furnace",8,6,-1,false,0)};
        var delta=GuiScene.messages(5,2,"Furnace",rect,moved,initial,JsonNull.INSTANCE,false);
        check(delta.get(1).getAsJsonObject("changes").size()==1,"Unchanged GUI instructions not resent");
        scene.accept(delta.get(0));scene.accept(delta.get(1));
        check(scene.commands()[1].get(3).getAsString().equals("Chest"),"Pending GUI not visible");
        scene.accept(delta.get(2));
        check(scene.commands()[1].get(3).getAsString().equals("Furnace"),"GUI change commits");
        check(!scene.accept(full.get(0)),"Stale GUI revision ignored");
        GuiScene late=new GuiScene(5,98);
        for (var p:GuiScene.messages(5,99,"Furnace",rect,moved,new JsonArray[0],JsonNull.INSTANCE,true)) late.accept(p);
        check(late.ready&&Arrays.equals(late.commands(),moved),"Late observer receives full current scene");
        var shortened=GuiScene.messages(5,100,"New tab",rect,new JsonArray[]{initial[0]},moved,JsonNull.INSTANCE,false);
        for (var p:shortened) late.accept(p);
        check(late.commands().length==1,"Tab switch removes old drawing instructions");
        rejects(()->GuiScene.validateCommand(a("b",null,null,"https://example.invalid/image.png",0,0,1,1,0,1,0,1,-1)));
        rejects(()->GuiScene.validateCommand(a("s",null,null,"sodium:gui/options",0,0,0,0,0,0,12,12,-1)));
        for(String icon:new String[]{"face","modrinth","curseforge","info","github","issues","cursor/food","cursor/air","cursor/search","cursor/withered","cursor/heart","cursor/bug"}) {
            GuiScene.validateCommand(a("b",null,null,"wigo:textures/gui/"+icon+".png",0,0,16,16,0,1,0,1,-1));
            check(true,"Packaged F8 icon is allowed: "+icon);
        }
        rejects(()->GuiScene.validateCommand(a("b",null,null,"wigo:textures/gui/unknown.png",0,0,16,16,0,1,0,1,-1)));
        rejects(()->GuiScene.validateCommand(a("b",null,null,"wigo:textures/gui/cursor/unknown.png",0,0,16,16,0,1,0,1,-1)));
        rejects(()->GuiScene.validateCommand(a("b",null,null,"other:textures/gui/face.png",0,0,16,16,0,1,0,1,-1)));
        rejects(()->GuiScene.validateCommand(a("t",null,null,"Wrong arguments")));
        rejects(()->GuiScene.validateCommand(a("r",null,null,Float.NaN,0,10,10,-1,-1)));
        rejects(()->GuiScene.validateCommand(a("q",null,null,"x".repeat(257))));
        GuiScene.validateCommand(a("o",null,null,"minecraft:oak",a(0,0,128,128),62.5,true));
        check(true,"Native sign model has a compact semantic instruction");
        rejects(()->GuiScene.validateCommand(a("o",null,null,"other:oak",a(0,0,128,128),62.5,true)));
        rejects(()->GuiScene.validateCommand(a("o",null,null,"minecraft:textures/invalid",a(0,0,128,128),62.5,true)));
        JsonArray glyphs=a("t",null,null,"abc",8,8,-1,false,0,a(a("abc","minecraft:alt",null,0)));
        GuiScene.validateCommand(glyphs);
        GuiScene styled=new GuiScene(7,0);
        for(var p:GuiScene.messages(7,1,"Glyphs",rect,new JsonArray[]{glyphs},new JsonArray[0],JsonNull.INSTANCE,true))styled.accept(p);
        check(styled.commands()[0].equals(glyphs),"Font and visual style survive the GUI protocol");
        rejects(()->GuiScene.validateCommand(a("t",null,null,"abc",8,8,-1,false,0,a(a("xyz","minecraft:alt",null,0)))));
        rejects(()->GuiScene.validateCommand(a("t",null,null,"abc",8,8,-1,false,0,a(a("abc","https://example.invalid/font",null,0)))));
        rejects(()->GuiScene.validateCommand(a("t",null,null,"abc",8,8,-1,false,0,a(a("abc","minecraft:alt",null,64)))));
        byte[] palette=new byte[16384];new Random(13).nextBytes(palette);
        String encoded=MapPalette.encode(palette,4096);
        check(Arrays.equals(Arrays.copyOfRange(palette,4096,8192),MapPalette.decode(encoded)),"Map colour IDs round trip exactly");
        rejects(()->MapPalette.decode(encoded.substring(0,encoded.length()-4)));
        JsonArray map=a("m",null,null,32,encoded);GuiScene.validateCommand(map);
        check(map.toString().length()<GuiScene.MAX_COMMAND_CHARS,"Worst case map strip stays bounded");
        GuiScene missing=new GuiScene(6,0);
        var incomplete=GuiScene.messages(6,1,"Missing",rect,initial,new JsonArray[0],JsonNull.INSTANCE,true);
        missing.accept(incomplete.get(0));rejects(()->missing.accept(incomplete.get(2)));
        MirrorState unsupported=new MirrorState();
        unsupported.accept(new com.google.gson.JsonParser().parse("{op:open,session:8,kind:'unsupported',rows:0,size:0,name:'Actor',yaw:0}").getAsJsonObject());
        unsupported.accept(new com.google.gson.JsonParser().parse("{op:commit,session:8,revision:1}").getAsJsonObject());
        check(unsupported.ready&&unsupported.slotCount==0,"Unsupported menu has no old items");
        System.out.println("WIGO GUI protocol: "+checks+" checks passed");
    }
    private static void copies() {
        JsonArray[] old=new JsonArray[40];for(int index=0;index<old.length;index++)old[index]=a("t",null,null,"Line "+index,8,index*4,-1,false,0);
        var scene=new GuiScene(71,0);var rect=a(0,0,176,166);
        for(var packet:GuiScene.messages(71,1,new JsonPrimitive("Rows"),rect,old,new JsonArray[0],JsonNull.INSTANCE,true,true,true))scene.accept(packet);
        JsonArray[] inserted=new JsonArray[42];System.arraycopy(old,0,inserted,0,10);inserted[10]=a("t",null,null,"Added A",8,8,-1,false,0);inserted[11]=a("t",null,null,"Added B",8,8,-1,false,0);System.arraycopy(old,10,inserted,12,30);
        var compact=GuiScene.messages(71,2,new JsonPrimitive("Rows"),rect,inserted,old,JsonNull.INSTANCE,false,true,true);
        var plain=GuiScene.messages(71,2,new JsonPrimitive("Rows"),rect,inserted,old,JsonNull.INSTANCE,false,true,false);
        check(compact.size()==1&&compact.get(0).has("copies")&&compact.get(0).toString().length()<plain.get(0).toString().length(),"Inserted rows resent the unchanged suffix");
        for(var packet:compact)scene.accept(packet);check(Arrays.equals(scene.commands(),inserted),"Insertion copies changed ordering");
        for(var packet:GuiScene.messages(71,3,new JsonPrimitive("Rows"),rect,old,inserted,JsonNull.INSTANCE,false,true,true))scene.accept(packet);
        check(Arrays.equals(scene.commands(),old),"Deletion copies changed ordering");
        for(String ranges:new String[]{"[[39,0,2]]","[[0,0,41]]","[[-1,0,1]]","[[0,0,0]]","[[0,0,1],[1,0,1]]","[[1.5,0,1]]"}) {
            var update=new com.google.gson.JsonParser().parse("{op:ui_update,session:71,revision:4,count:40,title:Rows,full:false,rect:[0,0,176,166],changes:{},copies:"+ranges+"}").getAsJsonObject();
            rejects(()->scene.accept(update));check(scene.revision==3&&Arrays.equals(scene.commands(),old),"Invalid range partially changed the panel");
        }
        for(var packet:GuiScene.messages(71,4,new JsonPrimitive("Rows"),rect,inserted,old,JsonNull.INSTANCE,true,true,true))check(!packet.has("copies"),"Full snapshots depend on cached commands");
    }
    private static void entityTransform() {
        var preview=new JsonObject();preview.addProperty("type","minecraft:armor_stand");preview.add("equipment",new JsonObject());
        preview.add("angles",a(210,25,0,0));preview.add("stand",a(true,true));preview.add("transform",a(a(0,0,0),a(0,0,1,0),null));
        var command=a("e",null,null,17,preview,a(120,20,165,80),25,141,75);
        GuiScene.validateCommand(command);
        var scene=new GuiScene(72,0);
        for(var packet:GuiScene.messages(72,1,"Smithing",a(0,0,176,166),new JsonArray[]{command},new JsonArray[0],JsonNull.INSTANCE,true))scene.accept(packet);
        check(scene.commands()[0].equals(command),"Native preview anchor, quaternion, pose and equipment flags round trip");
        for(JsonArray invalid:new JsonArray[]{a(a(0,0),a(0,0,1,0),null),a(a(0,0,0),a(0,0,1),null),a(a(0,0,0),a(0,0,1,0),a(0,0,0)),a(a("NaN",0,0),a(0,0,1,0),null)}) {
            var bad=dev.wigo.protocol.LegacyJson.copy(command);bad.get(4).getAsJsonObject().add("transform",invalid);rejects(()->GuiScene.validateCommand(bad));
        }
        var invalidAngles=dev.wigo.protocol.LegacyJson.copy(command);invalidAngles.get(4).getAsJsonObject().add("angles",a(0,1,2));rejects(()->GuiScene.validateCommand(invalidAngles));
        var invalidFlags=dev.wigo.protocol.LegacyJson.copy(command);invalidFlags.get(4).getAsJsonObject().add("stand",a(true,"false"));rejects(()->GuiScene.validateCommand(invalidFlags));
    }
    private static JsonArray a(Object... values) {JsonArray a=new JsonArray();for (Object v:values) a.add(v==null?JsonNull.INSTANCE:new Gson().toJsonTree(v));return a;}
    private static void check(boolean value,String message) {checks++;if (!value) throw new AssertionError(message);}
    private static void rejects(Runnable action) {try {action.run();throw new AssertionError("Invalid GUI accepted");}catch(IllegalArgumentException|UnsupportedOperationException expected){checks++;}}
}
