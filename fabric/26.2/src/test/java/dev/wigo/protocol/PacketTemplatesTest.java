package dev.wigo.protocol;

import com.google.gson.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import java.util.*;

public final class PacketTemplatesTest {
    private static int checks;
    public static void run() {
        var writer=new CompactCodec.Cache();var reader=new CompactCodec.Cache();
        for(boolean wide:new boolean[]{false,true}) {
            for(String text:List.of("Inventory","Пользовательский текст 👋","漢字日本語中文界".repeat(100),"a\n\"b\"\u0000", ""))round(new JsonPrimitive(text),writer,reader,wide);
            for(int id=0;id<PacketTemplates.DRAW.size();id++) {
                var command=new JsonArray();command.add(PacketTemplates.DRAW.get(id));command.add(JsonNull.INSTANCE);command.add(JsonNull.INSTANCE);
                for(int field=3;field<PacketTemplates.LENGTHS[id];field++)command.add(field==3?new JsonPrimitive("Literal"):new JsonPrimitive(field));
                round(command,writer,reader,wide);command.set(1,JsonParser.parseString("[1.0,0.0,0.0,1.0,8.0,9.0]"));command.set(2,JsonParser.parseString("[0,0,40,50]"));round(command,writer,reader,wide);
            }
            var styled=JsonParser.parseString("[\"t\",null,null,\"Привет漢字\",8,8,-1,false,0,[[\"Привет\",\"minecraft:default\",-1,0],[\"漢字\",\"minecraft:alt\",-1,1]],[0,40,80]]");
            round(styled,writer,reader,wide);
            var nonliteral=styled.deepCopy().getAsJsonArray();nonliteral.set(3,ProtocolText.reference(WireDictionary.key("container.inventory"),0,new JsonArray(),false));round(nonliteral,writer,reader,wide);
            for(var schema:PacketTemplates.OBJECTS) {
                var object=new JsonObject();for(var key:schema.keys())object.add(key,key.equals("op")&&schema.op()!=null?new JsonPrimitive(schema.op()):new JsonPrimitive(key));round(object,writer,reader,wide);
                object.addProperty("unknown future field",true);round(object,writer,reader,wide);
            }
        }
        var random=new Random(31);
        for(int count=0;count<1600;count++) {
            var object=new JsonObject();object.addProperty("op","ui_patch");object.addProperty("session",count/64);object.addProperty("revision",count);
            var changes=new JsonObject();
            for(int row=0;row<5;row++) {
                var command=new JsonArray();command.add("r");command.add(JsonNull.INSTANCE);command.add(JsonNull.INSTANCE);
                for(int field=3;field<9;field++)command.add(field<7?random.nextInt(512):0xffaaaaaa);changes.add(Integer.toString(row),command);
            }
            object.add("changes",changes);round(object,writer,reader,true);
            check(writer.storedChars()<=262144&&reader.storedChars()<=262144,"Cache quota grew");
        }
        for(byte[] invalid:List.of(new byte[]{26,1,0},new byte[]{26,2,0,(byte)0xd8},new byte[]{28,(byte)255,0},new byte[]{28,0,64},new byte[]{28,1,48},new byte[]{29,(byte)255}))reject(()->CompactCodec.decode(invalid,new CompactCodec.Cache(),16384));
        check(NetworkControl.supportsPacketTemplates(NetworkControl.hello()),"Native capability absent");
        for(String value:List.of("0","2","true","\"1\"","1.0","null")) {
            var hello=NetworkControl.hello();hello.add("packetFormat",JsonParser.parseString(value));check(!NetworkControl.supportsPacketTemplates(hello),"Invalid capability accepted");
        }
        var peer=new NetworkSessions.Peer();var target=new NetworkSessions.Peer();peer.negotiate(new NetworkSessions.Options(true,true));peer.shortText=peer.richText=peer.packetTemplates=true;
        for(int tick=0;tick<2100;tick++) {
            String text="{\"op\":\"cursor\",\"actor\":\"12345678-1234-4321-9999-123456789abc\",\"session\":7,\"x\":"+tick+",\"y\":20,\"hover\":-1}";
            var bytes=new FriendlyByteBuf(Unpooled.buffer());
            try {
                try(var scope=NetworkSessions.scope(peer)){new CompressedText.Payload(text,16384).write(bytes);}
                try(var scope=NetworkSessions.scope(target)){check(CompressedText.read(bytes,16384).json().equals(text),"Native packet/counter reset changed data");}
            }finally{bytes.release();}
        }
        System.out.println("WIGO packet templates: "+checks+" checks passed; exact schemas, styled text, UTF-16, bounds, cache eviction and native resets");
    }
    private static void round(JsonElement json,CompactCodec.Cache writer,CompactCodec.Cache reader,boolean wide){byte[] bytes=CompactCodec.encode(json,writer,true,true,wide);JsonElement result=CompactCodec.decode(bytes,reader,16384);check(result.toString().equals(json.toString()),"Template changed value/order: "+json);}
    private static void reject(Runnable action){try{action.run();throw new AssertionError("Malformed template accepted");}catch(io.netty.handler.codec.DecoderException|IllegalArgumentException expected){checks++;}}
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
