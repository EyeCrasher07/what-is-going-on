package dev.wigo.protocol;

import com.google.gson.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import java.util.*;

public final class CompactCodecTest {
    private static int checks;
    public static void run() {
        var encoder=new CompactCodec.Cache();var decoder=new CompactCodec.Cache();
        for(String source:List.of("null","true","false","0","-1","9223372036854775807","-9223372036854775808","1.0","-0.0","0.25","1.25","1.23456789","1e-12","\"123\"","\"001\"","\"minecraft:diamond\"","\"Привет 👋 日本語\""))roundtrip(source,encoder,decoder);
        String first="{\"op\":\"patch\",\"actor\":\"12345678-1234-4321-9999-123456789abc\",\"session\":7,\"revision\":1,\"slots\":{\"5\":{\"id\":\"minecraft:stone\",\"count\":64}}}";
        byte[] initial=roundtrip(first,encoder,decoder),changed=roundtrip(first.replace(":64",":63").replace("revision\":1","revision\":2"),encoder,decoder);
        check(encoder.patches>0&&changed.length<initial.length,"Count-only changes do not use field patches");
        byte[] again=roundtrip(first,encoder,decoder);check(again.length<initial.length&&encoder.references>0,"Repeated descriptions were not cached");
        textTemplates();translations();
        Random random=new Random(19);
        for(int i=0;i<2200;i++) {
            JsonObject json=new JsonObject();json.addProperty("op","update");json.addProperty("session",i/100);json.addProperty("revision",i);
            JsonArray commands=new JsonArray();
            for(int j=0;j<4;j++)commands.add(new com.google.gson.JsonParser().parse("[\"t\",null,null,\"Row "+random.nextInt(250)+"\",8,"+(j*10)+",-1,false,0]"));
            json.add("commands",commands);roundtrip(json.toString(),encoder,decoder);
            check(encoder.storedChars()<=262144&&decoder.storedChars()<=262144,"Connection cache exceeds its bound");
        }
        var sender=new NetworkSessions.Peer();var receiver=new NetworkSessions.Peer();
        sender.negotiate(new NetworkSessions.Options(true,false));sender.negotiate(NetworkSessions.Options.LEGACY);
        check(sender.negotiations==1&&sender.options.compact(),"Negotiated method changed within a session");
        for(int i=0;i<2100;i++) {
            String json=first.replace("revision\":1","revision\":"+(i+1));
            byte[] bytes=wire(json,sender,false);read(bytes,receiver,json);
            if(i%100==0)read(wire(json,sender,true),receiver,json);
        }
        byte[] valid=wire(first,sender,false),corrupt=Arrays.copyOf(valid,valid.length-1);
        rejects(()->read(corrupt,receiver,first));sender.resetEncoder();read(wire(first,sender,false),receiver,first);
        rejects(()->CompactCodec.decode(new byte[]{12,1},new CompactCodec.Cache(),16384));
        rejects(()->CompactCodec.decode(new byte[]{7,2,(byte)0xc0,(byte)0xaf},new CompactCodec.Cache(),16384));
        rejects(()->CompactCodec.decode(new byte[]{5,0x7f,(byte)0xc0,0,0},new CompactCodec.Cache(),16384));
        rejects(()->CompactCodec.decode(new byte[]{6,0x7f,(byte)0xf0,0,0,0,0,0,0},new CompactCodec.Cache(),16384));
        var small=new CompactCodec.Cache();byte[] expanded=CompactCodec.encode(new com.google.gson.JsonParser().parse("\""+"a".repeat(1000)+"\""),small);
        rejects(()->CompactCodec.decode(expanded,new CompactCodec.Cache(),100));
        check(NetworkSessions.current()==null,"Network context leaked from codec");
        atomic();
        System.out.println("WIGO compact protocol: "+checks+" checks passed; first/count-change/cached bytes "+initial.length+"/"+changed.length+"/"+again.length);
    }
    private static void textTemplates() {
        var encoder=new CompactCodec.Cache();var decoder=new CompactCodec.Cache();
        byte[] first=roundtrip(new JsonPrimitive("Уровень зачарования: 5 👋").toString(),encoder,decoder);
        byte[] next=roundtrip(new JsonPrimitive("Уровень зачарования: 6 👋").toString(),encoder,decoder);
        check(next.length<first.length&&encoder.patches>0,"Changing-number text was not patched with translation IDs disabled");
        for(String value:List.of("Уровень зачарования: 6 👌","Уровень зачарования: 16 👌","Уровень зачарования: 👋16 👌","Уровень зачарования: 👌16 👌"))
            roundtrip(new JsonPrimitive(value).toString(),encoder,decoder);
        for(int i=0;i<1200;i++)roundtrip(new JsonPrimitive("Repeated description "+(i%29)+" 👋 score="+i+" 日本語").toString(),encoder,decoder);
    }
    private static void translations() {
        ProtocolText.Provider old=ProtocolText.provider();int key=WireDictionary.key("container.inventory"),language=WireDictionary.language("sr_sp");
        var reference=ProtocolText.reference(key,language,new JsonArray(),false);
        ProtocolText.provider(new ProtocolText.Provider() {
            public JsonElement reference(String text,boolean visual){return text.equals("Инвентар")?ProtocolText.reference(key,language,new JsonArray(),visual):null;}
            public String resolve(int id,int locale,JsonArray args,boolean visual){check(id==key&&locale==language&&args.size()==0,"Caption IDs depend on the viewer's language");return "Инвентар";}
        });
        try {
            var original=new com.google.gson.JsonParser().parse("{\"op\":\"ui_update\",\"title\":\"Инвентар\",\"labels\":{\"inventory\":\"Инвентар\"},\"changes\":{\"0\":[\"t\",null,null,\"Инвентар\",8,8,-1,false,0]},\"carried\":{\"components\":{\"minecraft:custom_data\":{\"title\":\"Инвентар\",\"$text\":[0,0,[],false]}}}}").getAsJsonObject();
            JsonElement encoded=ProtocolText.encode(original,true);
            check(ProtocolText.isReference(encoded.getAsJsonObject().get("title")),"Whole caption did not receive a shared translation-key ID");
            check(ProtocolText.isReference(encoded.getAsJsonObject().getAsJsonObject("labels").get("inventory")),"Fast inventory caption remained literal");
            check(encoded.getAsJsonObject().get("carried").equals(original.get("carried")),"Translation transform changed custom item data");
            check(ProtocolText.resolve(encoded).equals(original),"Source-language captions did not roundtrip");
            check(ProtocolText.encode(original,false).equals(original),"Disabled translation IDs changed data");
            var custom=new com.google.gson.JsonParser().parse("{\"op\":\"ui_update\",\"title\":\"My storage\",\"changes\":{\"0\":[\"q\",null,null,\"Инвентар\"]},\"carried\":{\"components\":{\"minecraft:custom_data\":{\"embedded\":{\"$text\":[0,0,[],false]}}}}}");
            check(ProtocolText.encode(custom,true).equals(custom)&&ProtocolText.resolve(custom).equals(custom),"Caption transform changed a custom name, chat draft or typed-looking item data");
            var enc=new CompactCodec.Cache();var dec=new CompactCodec.Cache();roundtrip(encoded.toString(),enc,dec);
            for(int locale:new int[]{WireDictionary.language("en_us"),language,WireDictionary.language("ru_ru")})
                roundtrip(ProtocolText.reference(key,locale,new JsonArray(),true).toString(),enc,dec);
            for(String invalid:List.of("{\"$text\":[-1,0,[],false]}","{\"$text\":[0,0,{},false]}","{\"$text\":[0,0,[42],false]}","{\"$text\":[0,0.0,[],false]}"))
                check(!ProtocolText.isReference(new com.google.gson.JsonParser().parse(invalid)),"Malformed translation reference accepted");
            check(ProtocolText.isReference(reference),"Valid translation reference rejected");
        }finally {ProtocolText.provider(old);}
    }
    private static void atomic() {
        var scene=new GuiScene(1,0);JsonArray rect=new com.google.gson.JsonParser().parse("[0,0,176,166]").getAsJsonArray();
        JsonArray[] commands={new com.google.gson.JsonParser().parse("[\"t\",null,null,\"Inventory\",8,8,-1,false,0]").getAsJsonArray()};
        var messages=GuiScene.messages(1,1,new JsonPrimitive("Inventory"),rect,commands,new JsonArray[0],JsonNull.INSTANCE,true,true);
        check(messages.size()==1&&scene.accept(messages.get(0))&&scene.ready,"Small GUI update was not atomic");
        var broken=dev.wigo.protocol.LegacyJson.copy(messages.get(0));broken.addProperty("revision",2);broken.getAsJsonObject("changes").add("1",commands[0]);
        rejects(()->scene.accept(broken));check(scene.revision==1&&scene.commands()[0].get(3).getAsString().equals("Inventory"),"Invalid atomic GUI update changed the scene");
        var state=new MirrorState();state.accept(new com.google.gson.JsonParser().parse("{op:open,session:1,kind:inventory,rows:0,size:46,name:Actor,yaw:0}").getAsJsonObject());
        var update=new com.google.gson.JsonParser().parse("{op:update,session:1,revision:1,slots:{5:{id:'minecraft:stone',count:63}},carried:null}").getAsJsonObject();
        check(state.accept(update)&&state.slot(5).getAsJsonObject().get("count").getAsInt()==63,"Atomic item update");
        update.addProperty("revision",2);update.addProperty("title","x".repeat(257));update.getAsJsonObject("slots").add("5",JsonNull.INSTANCE);
        rejects(()->state.accept(update));check(state.revision==1&&!state.slot(5).isJsonNull(),"Invalid atomic item metadata changed slots");
        var large=new JsonArray[20];
        for(int i=0;i<large.length;i++)large[i]=new com.google.gson.JsonParser().parse("[\"t\",null,null,\""+"a".repeat(1800)+i+"\",8,8,-1,false,0]").getAsJsonArray();
        var parts=GuiScene.messages(2,1,new JsonPrimitive("Large menu"),rect,large,new JsonArray[0],JsonNull.INSTANCE,true,true);
        check(parts.size()>3,"Large compact GUI did not retain staged transfer");var expanded=new GuiScene(2,0);
        for(var part:parts){check(part.toString().length()<=Wire.MAX_TEXT,"Staged compact GUI exceeded the packet bound");expanded.accept(part);}
        check(expanded.ready&&Arrays.equals(large,expanded.commands()),"Large staged compact GUI did not commit atomically");
    }
    private static byte[] roundtrip(String source,CompactCodec.Cache encoder,CompactCodec.Cache decoder) {
        JsonElement json=new com.google.gson.JsonParser().parse(source);byte[] bytes=CompactCodec.encode(json,encoder);
        check(CompactCodec.decode(bytes,decoder,16384).toString().equals(json.toString()),"Compact tree changed "+source);return bytes;
    }
    private static byte[] wire(String json,NetworkSessions.Peer sender,boolean legacy) {
        var buf=new FriendlyByteBuf(Unpooled.buffer());
        try(var scope=NetworkSessions.scope(sender)) {
            var payload=new CompressedText.Payload(json,16384);if(legacy)payload.legacy();payload.write(buf);
            byte[] bytes=new byte[buf.readableBytes()];buf.readBytes(bytes);return bytes;
        }finally {buf.release();}
    }
    private static void read(byte[] bytes,NetworkSessions.Peer receiver,String json) {
        var buf=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try(var scope=NetworkSessions.scope(receiver)){check(CompressedText.read(buf,16384).json().equals(json),"Compact frame changed JSON");}
        finally {buf.release();}
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void rejects(Runnable task){try{task.run();throw new AssertionError("Invalid compact data accepted");}catch(RuntimeException expected){checks++;}}
}
