package dev.wigo.protocol;

import com.google.gson.*;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.Deflater;

public final class CompressedTextTest {
    private static int checks;
    public static void run() {
        roundTrip("",1024);roundTrip("{\"op\":\"close\"}",1024);
        String move="{\"op\":\"patch\",\"actor\":\"12345678-1234-4321-9999-123456789abc\",\"session\":7,\"revision\":2,\"slots\":{\"9\":null,\"10\":{\"id\":\"minecraft:diamond\",\"count\":12}}}";
        String inventory=inventory();
        roundTrip(move,Wire.MAX_TEXT);roundTrip(inventory,Wire.MAX_TEXT);
        roundTrip("Русский чат 👋 English 日本語 e\u0301 ".repeat(40),Wire.MAX_TEXT);
        roundTrip("а".repeat(1024),1024);roundTrip("界".repeat(Wire.MAX_TEXT),Wire.MAX_TEXT);
        byte[] small=encode(new CompressedText.Payload("{\"op\":\"close\"}",1024));
        check(parts(small).original()==0,"Tiny messages bypass compression");
        Random random=new Random(7);StringBuilder noise=new StringBuilder();
        for(int i=0;i<128;i++) noise.append((char)random.nextInt(128));
        byte[] noisy=encode(new CompressedText.Payload(noise.toString(),1024));
        check(parts(noisy).original()==0,"Incompressible frame grows");
        roundTrip(noise.toString(),1024);
        var shared=new CompressedText.Payload(inventory,Wire.MAX_TEXT);
        try(var scope=CompressedText.minecraftCompression(256)) {
            check(parts(encode(shared)).original()==0,"Minecraft-compressed packets are double-compressed");
            try(var nested=CompressedText.minecraftCompression(-1)) {
                check(parts(encode(shared)).original()>0,"Custom compression unavailable without Minecraft compression");
            }
            check(parts(encode(shared)).original()==0,"Nested compression scope was not restored");
        }
        check(parts(encode(shared)).original()>0,"Compression scope leaked out of packet encoder");
        try(var scope=CompressedText.minecraftCompression(256)) {
            var payload=new CompressedText.Payload(move,Wire.MAX_TEXT);
            byte[] encodedMove=encode(payload);
            check(parts(encodedMove).original()>0,"Below-threshold message did not compress: raw="+move.length()+", encoded="+encodedMove.length+", original="+parts(encodedMove).original());
            FriendlyByteBuf buf=new FriendlyByteBuf(Unpooled.buffer());
            try {buf.writeZero(200);payload.write(buf);buf.readerIndex(200);check(buf.readVarInt()==0,"Actual packet header ignored in threshold decision");}
            finally {buf.release();}
        }
        rejects(()->new CompressedText.Payload("a".repeat(1025),1024));
        rejects(()->strict(encode(new CompressedText.Payload("a".repeat(1025),Wire.MAX_TEXT)),1024));
        rejects(()->strict(frame(-1,new byte[]{1}),1024));
        rejects(()->strict(frame(3073,new byte[]{1}),1024));
        rejects(()->strict(frame(20,new byte[0]),1024));
        rejects(()->strict(frame(0,new byte[]{(byte)0xC0,(byte)0xAF}),1024));
        byte[] trailing=Arrays.copyOf(small,small.length+1);
        rejects(()->strict(trailing,1024));
        Parts valid=parts(encode(new CompressedText.Payload(inventory,Wire.MAX_TEXT)));
        byte[] corrupt=valid.data().clone();corrupt[corrupt.length-1]^=1;
        rejects(()->strict(frame(valid.original(),corrupt),Wire.MAX_TEXT));
        rejects(()->strict(frame(valid.original(),Arrays.copyOf(valid.data(),valid.data().length-1)),Wire.MAX_TEXT));
        rejects(()->strict(frame(valid.original(),Arrays.copyOf(valid.data(),valid.data().length+1)),Wire.MAX_TEXT));
        rejects(()->strict(frame(valid.original()-1,valid.data()),Wire.MAX_TEXT));
        rejects(()->strict(frame(valid.original()+1,valid.data()),Wire.MAX_TEXT));
        rejects(()->strict(new byte[]{0,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,7},Wire.MAX_TEXT));
        byte[] bomb;
        Deflater compressor=new Deflater();
        try {compressor.setInput("a".repeat(100000).getBytes(StandardCharsets.UTF_8));compressor.finish();byte[] out=new byte[1024];int count=compressor.deflate(out);bomb=Arrays.copyOf(out,count);}
        finally {compressor.end();}
        rejects(()->strict(frame(1,bomb),Wire.MAX_TEXT));
        FriendlyByteBuf bad=new FriendlyByteBuf(Unpooled.wrappedBuffer(frame(valid.original(),corrupt)));
        try {check(CompressedText.readSafely(bad,Wire.MAX_TEXT).json().isEmpty()&&bad.readableBytes()==0,"Invalid frame fails Minecraft instead of dropping WIGO");}
        finally {bad.release();}
        FriendlyByteBuf extra=new FriendlyByteBuf(Unpooled.wrappedBuffer(trailing));
        try {check(CompressedText.readSafely(extra,1024).json().isEmpty()&&extra.readableBytes()==0,"Trailing data fails Minecraft instead of dropping WIGO");}
        finally {extra.release();}
        roundTrip(inventory,Wire.MAX_TEXT); // Reused inflater recovers after all rejected streams.
        concurrent(inventory);
        System.out.println("WIGO compression: "+checks+" checks passed; JSON/payload bytes with Minecraft compression disabled: move "+move.getBytes(StandardCharsets.UTF_8).length+"/"+encode(new CompressedText.Payload(move,Wire.MAX_TEXT)).length+", full inventory "+inventory.getBytes(StandardCharsets.UTF_8).length+"/"+encode(shared).length);
    }
    private static void concurrent(String json) {
        var payload=new CompressedText.Payload(json,Wire.MAX_TEXT);
        ExecutorService pool=Executors.newFixedThreadPool(4);
        try {
            List<Callable<byte[]>> tasks=new ArrayList<>();
            for(int i=0;i<32;i++) tasks.add(()->{
                byte[] bytes=encode(payload);
                if(!strict(bytes,Wire.MAX_TEXT).equals(json)) throw new AssertionError("Concurrent payload corruption");
                return bytes;
            });
            var results=pool.invokeAll(tasks);byte[] first=results.get(0).get();
            for(var result:results) if(!Arrays.equals(first,result.get())) throw new AssertionError("Shared payload differs by observer");
            check(true,"Shared packet concurrent encoding");
        } catch(Exception invalid) {throw new AssertionError(invalid);}
        finally {pool.shutdownNow();}
    }
    private static String inventory() {
        JsonObject json=JsonParser.parseString("{op:'patch',actor:'12345678-1234-4321-9999-123456789abc',session:7,revision:1}").getAsJsonObject();
        JsonObject slots=new JsonObject();String[] ids={"minecraft:diamond","minecraft:stone","minecraft:oak_log","minecraft:iron_ingot"};
        for(int i=0;i<46;i++) {
            if(i<9||i>=45) slots.add(Integer.toString(i),JsonNull.INSTANCE);
            else {JsonObject item=new JsonObject();item.addProperty("id",ids[(i-9)%4]);item.addProperty("count",i+1);slots.add(Integer.toString(i),item);}
        }
        json.add("slots",slots);return json.toString();
    }
    private static void roundTrip(String json,int limit) {check(strict(encode(new CompressedText.Payload(json,limit)),limit).equals(json),"Text changed in compression round trip");}
    private static byte[] encode(CompressedText.Payload payload) {
        FriendlyByteBuf buf=new FriendlyByteBuf(Unpooled.buffer());
        try {payload.write(buf);byte[] bytes=new byte[buf.readableBytes()];buf.readBytes(bytes);return bytes;}
        finally {buf.release();}
    }
    private static String strict(byte[] bytes,int limit) {
        FriendlyByteBuf buf=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {String text=CompressedText.read(buf,limit).json();if(buf.readableBytes()!=0) throw new DecoderException("Unread frame data");return text;}
        finally {buf.release();}
    }
    private static Parts parts(byte[] bytes) {
        FriendlyByteBuf buf=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {return new Parts(buf.readVarInt(),buf.readByteArray());} finally {buf.release();}
    }
    private static byte[] frame(int original,byte[] data) {
        FriendlyByteBuf buf=new FriendlyByteBuf(Unpooled.buffer());
        try {buf.writeVarInt(original);buf.writeByteArray(data);byte[] bytes=new byte[buf.readableBytes()];buf.readBytes(bytes);return bytes;} finally {buf.release();}
    }
    private static void rejects(Runnable action) {try {action.run();throw new AssertionError("Invalid compression data accepted");}catch(RuntimeException expected){checks++;}}
    private static void check(boolean valid,String message) {checks++;if(!valid) throw new AssertionError(message);}
    private record Parts(int original,byte[] data) {}
}
