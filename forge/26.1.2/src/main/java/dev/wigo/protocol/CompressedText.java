package dev.wigo.protocol;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import com.google.gson.JsonParser;

/** Bounded, independent zlib frames. Small or incompressible messages remain UTF-8. */
public final class CompressedText {
    private static final int MIN_INPUT_BYTES=128,MIN_SAVED_BYTES=8;
    private static final ThreadLocal<Zlib> ZLIB=ThreadLocal.withInitial(Zlib::new);
    private static final ThreadLocal<Integer> MINECRAFT_THRESHOLD=new ThreadLocal<>();
    private static final int COMPACT_MARKER=-5;
    private CompressedText() {}

    public static final class Payload {
        private final String json;
        private final int maxChars;
        private volatile Frame plain,packed;
        private boolean legacy;

        public Payload(String json,int maxChars) {
            if (json.length()>maxChars) throw new EncoderException("WIGO text length");
            this.json=json;this.maxChars=maxChars;
        }
        private Payload(String json,int maxChars,Frame frame) {
            this(json,maxChars);
            if(frame.originalLength()==0) plain=frame;else packed=frame;
        }
        public String json() {return json;}
        public Payload legacy(){legacy=true;return this;}

        public void write(FriendlyByteBuf buf) {
            NetworkSessions.Peer peer=NetworkSessions.current();
            if(!legacy&&peer!=null&&peer.options.compact()) {writeCompact(buf,peer);return;}
            Frame raw=plain();
            Integer threshold=MINECRAFT_THRESHOLD.get();
            // The caller's writer index includes the actual packet ID and payload identifier.
            boolean minecraftCompresses=threshold!=null&&threshold>=0&&buf.writerIndex()+size(raw)>=threshold;
            Frame value=minecraftCompresses?raw:packed();
            buf.writeVarInt(value.originalLength());buf.writeByteArray(value.bytes());
        }
        private void writeCompact(FriendlyByteBuf buf,NetworkSessions.Peer peer) {
            synchronized(peer.encoder) {
                boolean reset=peer.forceReset||peer.sentSequence%2048==0;
                CompactCodec.Cache original=reset?new CompactCodec.Cache():peer.encoder;
                var parsed=JsonParser.parseString(json);
                var translated=ProtocolText.encode(parsed,peer.clientSide&&peer.options.translations()&&peer.richSource,peer.richText&&peer.richSource);
                if(translated.toString().length()>maxChars)translated=parsed;
                Integer threshold=MINECRAFT_THRESHOLD.get();
                CompactCodec.Cache cache=original.fork();byte[] raw=CompactCodec.encode(translated,cache,peer.shortText);
                if(raw.length>maxChars*3)throw new EncoderException("WIGO binary length");
                Frame frame=compactFrame(raw,buf.writerIndex(),peer.sentSequence+1,threshold);
                // Small deltas already use cheap references/field patches. Re-encoding them
                // adds work and can displace useful definitions from ordinary menu snapshots.
                if(peer.packetTemplates&&raw.length>=4096) {
                    long best=wireCost(buf,frame,reset,peer.sentSequence+1,threshold);
                    try {
                        var candidateCache=original.fork();byte[] candidate=CompactCodec.encode(translated,candidateCache,peer.shortText,true,true);
                        if(candidate.length<=maxChars*3) {
                            var candidateFrame=compactFrame(candidate,buf.writerIndex(),peer.sentSequence+1,threshold);
                            long cost=wireCost(buf,candidateFrame,reset,peer.sentSequence+1,threshold);
                            if(cost<best){frame=candidateFrame;cache=candidateCache;}
                        }
                    }catch(IllegalArgumentException|EncoderException boundedCandidate) {
                        // A candidate hitting its bounds must not reject a valid original frame.
                    }
                }
                buf.writeVarInt(COMPACT_MARKER);buf.writeBoolean(reset);buf.writeVarLong(peer.sentSequence+1);
                buf.writeVarInt(frame.originalLength());buf.writeByteArray(frame.bytes());
                peer.encoder.assign(cache);peer.sentSequence++;peer.forceReset=false;
            }
        }
        public int encodedSize() {
            return size(packed());
        }
        private Frame plain() {
            Frame value=plain;
            if (value==null) synchronized(this) {
                value=plain;
                if (value==null) {
                    byte[] bytes=json.getBytes(StandardCharsets.UTF_8);
                    if (bytes.length>maxChars*3) throw new EncoderException("WIGO UTF-8 length");
                    plain=value=new Frame(0,bytes);
                }
            }
            return value;
        }
        private Frame packed() {
            Frame value=packed;
            if(value==null) synchronized(this) {value=packed;if(value==null) packed=value=pack(plain().bytes());}
            return value;
        }
    }
    private static Frame compactFrame(byte[] raw,int prefix,long sequence,Integer threshold) {
        int header=VarInt.getByteSize(COMPACT_MARKER)+1+longSize(sequence)+1+VarInt.getByteSize(raw.length);
        return threshold!=null&&threshold>=0&&prefix+header+raw.length>=threshold?new Frame(0,raw):pack(raw);
    }
    /** Compare the entire framed Minecraft packet, including its existing identifier and headers. */
    private static long wireCost(FriendlyByteBuf prefix,Frame frame,boolean reset,long sequence,Integer threshold) {
        var packet=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            packet.writeBytes(prefix,0,prefix.writerIndex());packet.writeVarInt(COMPACT_MARKER);packet.writeBoolean(reset);packet.writeVarLong(sequence);
            packet.writeVarInt(frame.originalLength());packet.writeByteArray(frame.bytes());
            int size=packet.readableBytes();
            if(threshold!=null&&threshold>=0) {
                if(size>=threshold) {
                    byte[] data=new byte[size];packet.getBytes(0,data);size=VarInt.getByteSize(size)+compressedLength(data);
                }else size++;
            }
            return VarInt.getByteSize(size)+size;
        }finally{packet.release();}
    }
    private static int compressedLength(byte[] raw) {
        Deflater deflater=ZLIB.get().minecraftEncoder();
        try {
            deflater.setInput(raw);deflater.finish();byte[] output=new byte[raw.length+1024];int count=deflater.deflate(output);
            if(!deflater.finished())throw new EncoderException("WIGO compression comparison bound");return count;
        }finally{deflater.reset();}
    }
    private static int longSize(long value){int size=1;while((value>>>=7)!=0)size++;return size;}

    public static Payload read(FriendlyByteBuf buf,int maxChars) {
        int maxBytes=maxChars*3;
        int original=buf.readVarInt();
        if(original==COMPACT_MARKER)return readCompact(buf,maxChars);
        int length=buf.readVarInt();
        if (original<0||original>maxBytes||length<0||length>maxBytes||length>buf.readableBytes())
            throw new DecoderException("WIGO frame length");
        if (original>0&&length==0) throw new DecoderException("Empty WIGO zlib frame");
        byte[] packed=new byte[length];buf.readBytes(packed);
        if (buf.isReadable()) throw new DecoderException("Trailing WIGO frame data");
        byte[] raw=original==0?packed:unpack(packed,original);
        String json;
        try {
            json=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(raw)).toString();
        } catch (CharacterCodingException invalid) {throw new DecoderException("WIGO UTF-8",invalid);}
        if (json.length()>maxChars) throw new DecoderException("WIGO text length");
        return new Payload(json,maxChars,new Frame(original,packed));
    }

    private static Payload readCompact(FriendlyByteBuf buf,int maxChars) {
        NetworkSessions.Peer peer=NetworkSessions.current();
        if(peer==null)throw new DecoderException("WIGO binary frame outside connection scope");
        boolean reset=buf.readBoolean();long sequence=buf.readVarLong();
        int original=buf.readVarInt(),length=buf.readVarInt(),maxBytes=maxChars*3;
        if(sequence<1||!reset&&sequence!=peer.receivedSequence+1||original<0||original>maxBytes
                ||length<0||length>maxBytes||length>buf.readableBytes())throw new DecoderException("WIGO binary frame bounds or sequence");
        byte[] bytes=new byte[length];buf.readBytes(bytes);
        if(buf.isReadable())throw new DecoderException("Trailing WIGO frame data");
        byte[] raw=original==0?bytes:unpack(bytes,original);
        CompactCodec.Cache cache=reset?new CompactCodec.Cache():peer.decoder.fork();
        String json=CompactCodec.decode(raw,cache,maxChars).toString();
        if(json.length()>maxChars)throw new DecoderException("WIGO decoded length");
        peer.decoder.assign(cache);peer.receivedSequence=sequence;
        return new Payload(json,maxChars);
    }

    /** Invalid WIGO payloads are dropped without failing the Minecraft packet decoder. */
    public static Payload readSafely(FriendlyByteBuf buf,int maxChars) {
        try {return read(buf,maxChars);}
        catch (RuntimeException invalid) {buf.skipBytes(buf.readableBytes());return new Payload(NetworkSessions.current()==null?"":"{\"op\":\"decode_error\"}",maxChars);}
    }

    private static int size(Frame value) {
        return VarInt.getByteSize(value.originalLength())+VarInt.getByteSize(value.bytes().length)+value.bytes().length;
    }

    public static CompressionScope minecraftCompression(int threshold) {return new CompressionScope(threshold);}
    public static final class CompressionScope implements AutoCloseable {
        private final Integer previous;
        private CompressionScope(int threshold) {previous=MINECRAFT_THRESHOLD.get();MINECRAFT_THRESHOLD.set(threshold);}
        @Override public void close() {if(previous==null) MINECRAFT_THRESHOLD.remove();else MINECRAFT_THRESHOLD.set(previous);}
    }

    private static Frame pack(byte[] raw) {
        if (raw.length<MIN_INPUT_BYTES) return new Frame(0,raw);
        // The scratch output never grows beyond the original message.
        byte[] packed=new byte[raw.length];
        Deflater encoder=ZLIB.get().encoder();
        try {
            encoder.setInput(raw);encoder.finish();int size=0;
            while (!encoder.finished()&&size<packed.length) {
                int count=encoder.deflate(packed,size,packed.length-size);
                if (count==0) break;
                size+=count;
            }
            int plainSize=1+VarInt.getByteSize(raw.length)+raw.length;
            int packedSize=VarInt.getByteSize(raw.length)+VarInt.getByteSize(size)+size;
            if (encoder.finished()&&plainSize-packedSize>=MIN_SAVED_BYTES)
                return new Frame(raw.length,Arrays.copyOf(packed,size));
            return new Frame(0,raw);
        } finally {encoder.reset();}
    }

    private static byte[] unpack(byte[] packed,int original) {
        // One extra byte detects a stream expanding beyond its declared size.
        byte[] raw=new byte[original+1];
        Inflater decoder=ZLIB.get().decoder();
        try {
            decoder.setInput(packed);int size=0;
            while (!decoder.finished()&&size<=original) {
                int count=decoder.inflate(raw,size,raw.length-size);
                if (count==0) break;
                size+=count;
            }
            if (size!=original||!decoder.finished()||decoder.getRemaining()!=0)
                throw new DecoderException("WIGO zlib size or trailing data");
            return Arrays.copyOf(raw,size);
        } catch (DataFormatException invalid) {throw new DecoderException("WIGO zlib data",invalid);}
        finally {decoder.reset();}
    }

    private record Frame(int originalLength,byte[] bytes) {}
    // Codec work stays on the calling network thread; buffers and dictionaries are reset per message.
    private static final class Zlib {
        private Deflater encoder;
        private Deflater minecraftEncoder;
        private Inflater decoder;
        Deflater encoder() {if(encoder==null) encoder=new Deflater(2);return encoder;}
        Deflater minecraftEncoder(){if(minecraftEncoder==null)minecraftEncoder=new Deflater();return minecraftEncoder;}
        Inflater decoder() {if(decoder==null) decoder=new Inflater();return decoder;}
    }
}
