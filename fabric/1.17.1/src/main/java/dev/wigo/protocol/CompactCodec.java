package dev.wigo.protocol;

import com.google.gson.*;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/** Lossless tree encoding with bounded connection-local definitions and field patches. */
public final class CompactCodec {
    private static final int NIL=0,FALSE=1,TRUE=2,INTEGER=3,DECIMAL=4,FLOAT=5,DOUBLE=6,STRING=7,
            STATIC=8,UUID_VALUE=9,ARRAY=10,OBJECT=11,REFERENCE=12,DEFINE=13,
            ARRAY_PATCH=14,OBJECT_PATCH=15,INTEGER_STRING=16,WHOLE_FLOAT=17,QUARTER_FLOAT=18,TEXT=19,TEXT_LANGUAGE=20,STRING_PATCH=21,
            SHORT_TEXT=22,SHORT_TEXT_LANGUAGE=23,WIDE_TEXT=24,WIDE_TEXT_LANGUAGE=25,
            UTF16_STRING=26,UTF16_PATCH=27,DRAW_TEMPLATE=28,OBJECT_TEMPLATE=29;
    private static final int SLOTS=512,MAX_ENTRY=4096,MAX_CACHED_CHARS=262144,MAX_DEPTH=32,MAX_NODES=32768;
    private CompactCodec(){}
    private record Entry(JsonElement value,String json,String shape){}

    public static final class Cache {
        private Entry[] entries=new Entry[SLOTS];
        private final Map<String,Integer> exact=new HashMap<>();
        private final Map<String,Deque<Integer>> shapes=new HashMap<>();
        private int next,eviction,chars,lastLanguage=-1;
        private int nextStructure,nextText=256;
        private final LinkedHashSet<Long> recentLarge=new LinkedHashSet<>();
        public long references,patches,definitions;
        public Cache fork(){Cache value=new Cache();value.assign(this);return value;}
        public void assign(Cache value) {
            entries=value.entries.clone();exact.clear();exact.putAll(value.exact);shapes.clear();
            value.shapes.forEach((key,ids)->shapes.put(key,new ArrayDeque<>(ids)));
            next=value.next;eviction=value.eviction;chars=value.chars;lastLanguage=value.lastLanguage;
            nextStructure=value.nextStructure;nextText=value.nextText;recentLarge.clear();recentLarge.addAll(value.recentLarge);
            references=value.references;patches=value.patches;definitions=value.definitions;
        }
        public void clear(){entries=new Entry[SLOTS];exact.clear();shapes.clear();recentLarge.clear();next=eviction=chars=nextStructure=0;nextText=256;lastLanguage=-1;}
        public int storedChars(){return chars;}
        private int reserve(){int id=next;next=(next+1)%SLOTS;return id;}
        private int reserve(JsonElement value,boolean templates) {
            if(!templates)return reserve();
            if(isString(value)){int id=nextText;nextText=256+(nextText-255)%256;return id;}
            int id=nextStructure;nextStructure=(nextStructure+1)%256;return id;
        }
        private boolean admitLarge(String json) {
            long fingerprint=((long)json.hashCode()<<32)^json.length();
            if(recentLarge.contains(fingerprint))return true;
            if(recentLarge.size()==128)recentLarge.remove(recentLarge.iterator().next());
            recentLarge.add(fingerprint);return false;
        }
        private Entry get(int id){if(id<0||id>=SLOTS||entries[id]==null)throw new DecoderException("Unknown WIGO definition");return entries[id];}
        private void remove(int id) {
            Entry old=entries[id];if(old==null)return;
            entries[id]=null;chars-=old.json.length();exact.remove(old.json,id);
            var list=shapes.get(old.shape);if(list!=null){list.remove(id);if(list.isEmpty())shapes.remove(old.shape);}
        }
        private void put(int id,JsonElement value) {
            if(id<0||id>=SLOTS)throw new DecoderException("WIGO definition index");
            String json=value.toString();if(json.length()>MAX_ENTRY)throw new DecoderException("WIGO definition length");
            remove(id);
            while(chars+json.length()>MAX_CACHED_CHARS){remove(eviction);eviction=(eviction+1)%SLOTS;}
            String shape=shape(value);Entry entry=new Entry(dev.wigo.protocol.LegacyJson.copy(value),json,shape);
            entries[id]=entry;chars+=json.length();exact.put(json,id);
            Deque<Integer> list=shapes.computeIfAbsent(shape,key->new ArrayDeque<>());list.addFirst(id);
            definitions++;
        }
        private int base(JsonElement value) {
            Deque<Integer> candidates=shapes.get(shape(value));if(candidates==null)return -1;
            boolean text=isString(value);int best=-1,bestCost=text?value.getAsString().getBytes(StandardCharsets.UTF_8).length+2:value.toString().length()-8,examined=0;
            for(int id:candidates) {
                if(++examined>8)break;Entry entry=entries[id];if(entry==null)continue;
                int cost;
                if(text&&isString(entry.value)) {var parts=textChange(entry.value.getAsString(),value.getAsString());if(parts[0]+parts[1]<4)continue;cost=8+value.getAsString().substring(parts[0],value.getAsString().length()-parts[1]).getBytes(StandardCharsets.UTF_8).length;}
                else {var changed=changes(entry.value,value);if(changed==null)continue;cost=8;for(JsonElement change:changed.values())cost+=change.toString().length()+2;}
                if(cost<bestCost){bestCost=cost;best=id;}
            }
            return best;
        }
    }

    private static String shape(JsonElement value) {
        if(isString(value))return "string";
        if(value.isJsonObject())return "o:"+String.join("\u0000",LegacyJson.keys(value.getAsJsonObject()));
        if(value.isJsonArray()) {
            JsonArray array=value.getAsJsonArray();
            return "a:"+array.size()+":"+(array.size()>0&&array.get(0).isJsonPrimitive()?array.get(0).toString():"");
        }
        return "literal";
    }
    private static boolean isString(JsonElement value){return value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString();}
    private static int[] textChange(String old,String next) {
        int prefix=0,suffix=0;while(prefix<old.length()&&prefix<next.length()&&old.charAt(prefix)==next.charAt(prefix))prefix++;
        if(prefix>0&&prefix<old.length()&&Character.isLowSurrogate(old.charAt(prefix)))prefix--;
        while(suffix<old.length()-prefix&&suffix<next.length()-prefix&&old.charAt(old.length()-suffix-1)==next.charAt(next.length()-suffix-1))suffix++;
        if(suffix>0&&suffix<old.length()&&Character.isLowSurrogate(old.charAt(old.length()-suffix)))suffix--;
        return new int[]{prefix,suffix};
    }
    private static LinkedHashMap<Integer,JsonElement> changes(JsonElement old,JsonElement next) {
        LinkedHashMap<Integer,JsonElement> result=new LinkedHashMap<>();
        if(old.isJsonArray()&&next.isJsonArray()) {
            var a=old.getAsJsonArray();var b=next.getAsJsonArray();if(a.size()!=b.size()||a.size()>32)return null;
            for(int i=0;i<a.size();i++)if(!a.get(i).toString().equals(b.get(i).toString()))result.put(i,b.get(i));
        }else if(old.isJsonObject()&&next.isJsonObject()) {
            var a=old.getAsJsonObject();var b=next.getAsJsonObject();
            if(a.size()>32||!new ArrayList<>(LegacyJson.keys(a)).equals(new ArrayList<>(LegacyJson.keys(b))))return null;
            int i=0;for(String key:LegacyJson.keys(a)){if(!a.get(key).toString().equals(b.get(key).toString()))result.put(i,b.get(key));i++;}
        }else return null;
        return result.isEmpty()?null:result;
    }

    public static byte[] encode(JsonElement json,Cache cache) {
        return encode(json,cache,false);
    }
    public static byte[] encode(JsonElement json,Cache cache,boolean shortText) {
        return encode(json,cache,shortText,false,false);
    }
    public static byte[] encode(JsonElement json,Cache cache,boolean shortText,boolean templates,boolean utf16) {
        FriendlyByteBuf output=new FriendlyByteBuf(Unpooled.buffer());
        try {new Writer(output,cache,shortText,templates,utf16).value(json,0,true);byte[] bytes=new byte[output.readableBytes()];output.readBytes(bytes);return bytes;}
        finally {output.release();}
    }
    public static JsonElement decode(byte[] bytes,Cache cache,int maxChars) {
        FriendlyByteBuf input=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {JsonElement value=new Reader(input,cache,maxChars).value(0);if(input.isReadable())throw new DecoderException("Trailing WIGO binary data");return value;}
        finally {input.release();}
    }

    private static final class Writer {
        final FriendlyByteBuf out;final Cache cache;final boolean shortText,templates,utf16;int nodes;
        Writer(FriendlyByteBuf out,Cache cache,boolean shortText,boolean templates,boolean utf16){this.out=out;this.cache=cache;this.shortText=shortText;this.templates=templates;this.utf16=utf16;}
        void value(JsonElement value,int depth,boolean define) {
            if(depth>MAX_DEPTH||++nodes>MAX_NODES)throw new IllegalArgumentException("WIGO tree limit");
            if(isString(value)) {int known=WireDictionary.id(value.getAsString());if(known>=0){out.writeByte(STATIC);out.writeVarInt(known);return;}}
            String json=value.toString();boolean eligible=define&&json.length()>=(isString(value)?8:16)&&json.length()<=MAX_ENTRY;
            if(eligible) {
                Integer existing=cache.exact.get(json);
                if(existing!=null){out.writeByte(REFERENCE);out.writeVarInt(existing);cache.references++;return;}
                if(templates&&!isString(value)&&json.length()>384&&!cache.admitLarge(json)){value(value,depth+1,false);return;}
                int base=shortText&&ProtocolText.isReference(value)?-1:cache.base(value),id=cache.reserve(value,templates);
                out.writeByte(DEFINE);out.writeVarInt(id);
                if(base>=0) {
                    Entry original=cache.get(base);
                    if(isString(value)) {
                        int[] parts=textChange(original.value.getAsString(),value.getAsString());String middle=value.getAsString().substring(parts[0],value.getAsString().length()-parts[1]);
                        boolean wide=wide(middle);out.writeByte(wide?UTF16_PATCH:STRING_PATCH);out.writeVarInt(base);
                        out.writeVarInt(parts[0]);out.writeVarInt(parts[1]);if(wide)utf16(middle);else utf(middle);
                        cache.patches++;cache.put(id,value);return;
                    }
                    var changed=changes(original.value,value);
                    out.writeByte(value.isJsonArray()?ARRAY_PATCH:OBJECT_PATCH);out.writeVarInt(base);
                    long mask=0;for(int index:changed.keySet())mask|=1L<<index;out.writeVarLong(mask);
                    for(JsonElement child:changed.values())value(child,depth+1,true);
                    cache.patches++;
                }else value(value,depth+1,false);
                cache.put(id,value);return;
            }
            if(value.isJsonNull()){out.writeByte(NIL);return;}
            if(ProtocolText.isReference(value)) {
                var ref=value.getAsJsonObject().getAsJsonArray("$text");int language=ref.get(1).getAsInt();
                if(shortText){
                    int key=ref.get(0).getAsInt(),alias=ShortTextDictionary.alias(key);
                    boolean changed=cache.lastLanguage!=language;
                    out.writeByte(alias>=0?(changed?SHORT_TEXT_LANGUAGE:SHORT_TEXT):(changed?WIDE_TEXT_LANGUAGE:WIDE_TEXT));
                    if(changed){out.writeByte(language);cache.lastLanguage=language;}
                    if(alias>=0)out.writeByte(alias);else out.writeShort(key);
                    JsonArray args=ref.get(2).getAsJsonArray();out.writeByte((ref.get(3).getAsBoolean()?1:0)|(args.size()==0?0:2));
                    if(args.size()!=0){out.writeVarInt(args.size());for(JsonElement arg:args)value(arg,depth+1,true);}
                    return;
                }
                boolean changed=cache.lastLanguage!=language;out.writeByte(changed?TEXT_LANGUAGE:TEXT);
                if(changed){out.writeShort(language);cache.lastLanguage=language;}
                out.writeShort(ref.get(0).getAsInt());out.writeBoolean(ref.get(3).getAsBoolean());
                JsonArray args=ref.get(2).getAsJsonArray();out.writeVarInt(args.size());
                for(JsonElement arg:args)value(arg,depth+1,true);return;
            }
            if(value.isJsonArray()) {
                var array=value.getAsJsonArray();int template=templates?PacketTemplates.draw(array):-1;
                if(template>=0){draw(array,template,depth);return;}
                out.writeByte(ARRAY);out.writeVarInt(array.size());
                for(var child:array)value(child,depth+1,true);return;
            }
            if(value.isJsonObject()) {
                var object=value.getAsJsonObject();int template=templates?PacketTemplates.object(object):-1;
                if(template>=0){out.writeByte(OBJECT_TEMPLATE);out.writeByte(template);var schema=PacketTemplates.OBJECTS.get(template);for(var key:schema.keys())if(!key.equals("op")||schema.op()==null)value(object.get(key),depth+1,true);return;}
                out.writeByte(OBJECT);out.writeVarInt(object.size());
                for(var entry:object.entrySet()){value(new JsonPrimitive(entry.getKey()),depth+1,true);value(entry.getValue(),depth+1,true);}return;
            }
            var primitive=value.getAsJsonPrimitive();
            if(primitive.isBoolean()){out.writeByte(primitive.getAsBoolean()?TRUE:FALSE);return;}
            if(primitive.isNumber()){number(primitive.toString());return;}
            String text=primitive.getAsString();int id=WireDictionary.id(text);
            if(id>=0){out.writeByte(STATIC);out.writeVarInt(id);return;}
            try {if(text.matches("[0-9]{1,9}")) {
                int number=Integer.parseInt(text);if(Integer.toString(number).equals(text)){out.writeByte(INTEGER_STRING);out.writeVarInt(number);return;}
            }}catch(NumberFormatException ignored){}
            if(text.length()==36)try {
                UUID uuid=UUID.fromString(text);if(uuid.toString().equals(text)) {
                    out.writeByte(UUID_VALUE);out.writeLong(uuid.getMostSignificantBits());out.writeLong(uuid.getLeastSignificantBits());return;
                }
            }catch(IllegalArgumentException ignored){}
            if(wide(text)){out.writeByte(UTF16_STRING);utf16(text);}else{out.writeByte(STRING);utf(text);}
        }
        boolean wide(String text){return templates&&utf16&&text.length()>4&&text.getBytes(StandardCharsets.UTF_8).length>text.length()*2+2;}
        void utf16(String text){byte[] bytes=text.getBytes(StandardCharsets.UTF_16LE);out.writeVarInt(bytes.length);out.writeBytes(bytes);}
        void draw(JsonArray array,int id,int depth) {
            int flags=(array.get(1).isJsonNull()?0:1)|(array.get(2).isJsonNull()?0:2)|((array.size()-PacketTemplates.LENGTHS[id])<<4);
            if(id==1&&PacketTemplates.styledLiteral(array))flags|=4;
            if(id==0&&array.get(7).toString().equals(array.get(8).toString()))flags|=8;
            out.writeByte(DRAW_TEMPLATE);out.writeByte(id);out.writeByte(flags);
            for(int index=1;index<array.size();index++) {
                if(index==1&&(flags&1)==0||index==2&&(flags&2)==0||index==3&&(flags&4)!=0||index==8&&(flags&8)!=0)continue;
                value(array.get(index),depth+1,true);
            }
        }
        void number(String text) {
            try {
                long integer=Long.parseLong(text);
                if(Long.toString(integer).equals(text)){out.writeByte(INTEGER);out.writeVarLong((integer<<1)^(integer>>63));return;}
            }catch(NumberFormatException ignored){}
            try {
                float number=Float.parseFloat(text);
                if(Float.isFinite(number)&&Float.toString(number).equals(text)) {
                    if(number==Math.rint(number)&&Math.abs(number)<10000000&&Float.toString((float)(int)number).equals(text)) {
                        out.writeByte(WHOLE_FLOAT);int n=(int)number;out.writeVarInt((n<<1)^(n>>31));return;
                    }
                    int quarter=(int)(number*4);
                    if(Math.abs(number)<10000000&&Float.toString(quarter/4f).equals(text)) {
                        out.writeByte(QUARTER_FLOAT);out.writeVarInt((quarter<<1)^(quarter>>31));return;
                    }
                    out.writeByte(FLOAT);out.writeFloat(number);return;
                }
                double precise=Double.parseDouble(text);
                if(Double.isFinite(precise)&&Double.toString(precise).equals(text)){out.writeByte(DOUBLE);out.writeDouble(precise);return;}
            }catch(NumberFormatException ignored){}
            out.writeByte(DECIMAL);utf(text);
        }
        void utf(String text){byte[] bytes=text.getBytes(StandardCharsets.UTF_8);out.writeVarInt(bytes.length);out.writeBytes(bytes);}
    }

    private static final class Reader {
        final FriendlyByteBuf in;final Cache cache;final int maxChars;int remaining,nodes;
        Reader(FriendlyByteBuf in,Cache cache,int maxChars){this.in=in;this.cache=cache;this.maxChars=maxChars;remaining=maxChars;}
        void consume(int chars){remaining-=chars;if(remaining<0)throw new DecoderException("WIGO expanded text length");}
        int size(){int count=in.readVarInt();if(count<0||count>4096)throw new DecoderException("WIGO collection size");return count;}
        JsonElement value(int depth) {
            if(depth>MAX_DEPTH||++nodes>MAX_NODES)throw new DecoderException("WIGO tree limit");
            int type=in.readUnsignedByte();
            if(type==REFERENCE){Entry entry=cache.get(in.readVarInt());consume(entry.json.length());cache.references++;return dev.wigo.protocol.LegacyJson.copy(entry.value);}
            if(type==DEFINE) {
                int id=in.readVarInt();if(id<0||id>=SLOTS)throw new DecoderException("WIGO definition index");
                JsonElement value=value(depth+1);cache.put(id,value);return value;
            }
            if(type==STRING_PATCH||type==UTF16_PATCH) {
                String old=cache.get(in.readVarInt()).value.getAsString();int prefix=in.readVarInt(),suffix=in.readVarInt();
                if(prefix<0||suffix<0||prefix>old.length()||suffix>old.length()-prefix
                        ||prefix>0&&prefix<old.length()&&Character.isLowSurrogate(old.charAt(prefix))
                        ||suffix>0&&suffix<old.length()&&Character.isLowSurrogate(old.charAt(old.length()-suffix)))throw new DecoderException("WIGO text patch bounds");
                JsonElement text=new JsonPrimitive(old.substring(0,prefix)+(type==UTF16_PATCH?utf16():utf())+old.substring(old.length()-suffix));consume(text.toString().length());cache.patches++;return text;
            }
            if(type==DRAW_TEMPLATE) {
                int id=in.readUnsignedByte(),flags=in.readUnsignedByte();
                if(id>=PacketTemplates.DRAW.size()||(flags&~63)!=0||id!=1&&(flags&52)!=0||id!=0&&(flags&8)!=0||id==1&&(flags>>>4)>2)throw new DecoderException("WIGO drawing template");
                int count=PacketTemplates.LENGTHS[id]+(flags>>>4),saved=remaining;remaining=maxChars;
                var array=new JsonArray();array.add(PacketTemplates.DRAW.get(id));
                for(int index=1;index<count;index++) {
                    if(index==1&&(flags&1)==0||index==2&&(flags&2)==0||index==3&&(flags&4)!=0)array.add(JsonNull.INSTANCE);
                    else if(index==8&&(flags&8)!=0)array.add(dev.wigo.protocol.LegacyJson.copy(array.get(7)));
                    else array.add(value(depth+1));
                }
                if((flags&4)!=0) {
                    if(count<10||!array.get(9).isJsonArray())throw new DecoderException("WIGO styled template");
                    var text=new StringBuilder();for(var run:array.get(9).getAsJsonArray()) {
                        if(!run.isJsonArray()||run.getAsJsonArray().size()!=4||!isString(run.getAsJsonArray().get(0)))throw new DecoderException("WIGO styled template run");
                        text.append(run.getAsJsonArray().get(0).getAsString());if(text.length()>2048)throw new DecoderException("WIGO styled template size");
                    }
                    array.set(3,new JsonPrimitive(text.toString()));
                }
                remaining=saved;consume(array.toString().length());return array;
            }
            if(type==OBJECT_TEMPLATE) {
                int id=in.readUnsignedByte();if(id>=PacketTemplates.OBJECTS.size())throw new DecoderException("WIGO object template");
                var schema=PacketTemplates.OBJECTS.get(id);int saved=remaining;remaining=maxChars;var object=new JsonObject();
                for(var key:schema.keys())object.add(key,key.equals("op")&&schema.op()!=null?new JsonPrimitive(schema.op()):value(depth+1));
                remaining=saved;consume(object.toString().length());return object;
            }
            if(type==ARRAY_PATCH||type==OBJECT_PATCH) {
                JsonElement base=dev.wigo.protocol.LegacyJson.copy(cache.get(in.readVarInt()).value);long mask=in.readVarLong();
                int count=type==ARRAY_PATCH?base.getAsJsonArray().size():base.getAsJsonObject().size();
                if(count>32||mask==0||(mask>>>count)!=0)throw new DecoderException("WIGO field patch mask");
                int saved=remaining;remaining=maxChars;
                var keys=type==OBJECT_PATCH?new ArrayList<>(LegacyJson.keys(base.getAsJsonObject())):List.<String>of();
                for(int i=0;i<count;i++)if((mask&(1L<<i))!=0) {
                    JsonElement next=value(depth+1);
                    if(type==ARRAY_PATCH)base.getAsJsonArray().set(i,next);else base.getAsJsonObject().add(keys.get(i),next);
                }
                remaining=saved;consume(base.toString().length());cache.patches++;return base;
            }
            if(type==ARRAY) {
                int count=size();consume(2+Math.max(0,count-1));JsonArray array=new JsonArray();
                for(int i=0;i<count;i++)array.add(value(depth+1));return array;
            }
            if(type==OBJECT) {
                int count=size();consume(2+Math.max(0,count-1)+count);JsonObject object=new JsonObject();
                for(int i=0;i<count;i++) {
                    JsonElement key=value(depth+1);if(!key.isJsonPrimitive()||!key.getAsJsonPrimitive().isString())throw new DecoderException("WIGO object key");
                    String name=key.getAsString();if(object.has(name))throw new DecoderException("Duplicate WIGO field");object.add(name,value(depth+1));
                }return object;
            }
            if(type==SHORT_TEXT||type==SHORT_TEXT_LANGUAGE||type==WIDE_TEXT||type==WIDE_TEXT_LANGUAGE){
                if(type==SHORT_TEXT_LANGUAGE||type==WIDE_TEXT_LANGUAGE)cache.lastLanguage=in.readUnsignedByte();
                int key=type==SHORT_TEXT||type==SHORT_TEXT_LANGUAGE?ShortTextDictionary.key(in.readUnsignedByte()):in.readUnsignedShort();
                int flags=in.readUnsignedByte();if((flags&~3)!=0)throw new DecoderException("WIGO text flags");
                int count=(flags&2)==0?0:size();if(count>16||(flags&2)!=0&&count==0)throw new DecoderException("WIGO text arguments");
                int saved=remaining;remaining=maxChars;JsonArray args=new JsonArray();for(int i=0;i<count;i++)args.add(value(depth+1));
                JsonElement text=ProtocolText.reference(key,cache.lastLanguage,args,(flags&1)!=0);remaining=saved;consume(text.toString().length());return text;
            }
            if(type==TEXT||type==TEXT_LANGUAGE) {
                if(type==TEXT_LANGUAGE)cache.lastLanguage=in.readUnsignedShort();
                int key=in.readUnsignedShort();boolean visual=in.readBoolean();int count=size();if(count>16)throw new DecoderException("WIGO translation arguments");
                int saved=remaining;remaining=maxChars;JsonArray args=new JsonArray();for(int i=0;i<count;i++)args.add(value(depth+1));
                JsonElement text=ProtocolText.reference(key,cache.lastLanguage,args,visual);remaining=saved;consume(text.toString().length());return text;
            }
            JsonElement scalar=switch(type) {
                case NIL->JsonNull.INSTANCE;case FALSE->new JsonPrimitive(false);case TRUE->new JsonPrimitive(true);
                case INTEGER->{long value=in.readVarLong();yield new com.google.gson.JsonParser().parse(Long.toString((value>>>1)^-(value&1)));}
                case WHOLE_FLOAT->{int value=in.readVarInt();yield new com.google.gson.JsonParser().parse(Float.toString((float)((value>>>1)^-(value&1))));}
                case QUARTER_FLOAT->{int value=in.readVarInt();yield new com.google.gson.JsonParser().parse(Float.toString(((value>>>1)^-(value&1))/4f));}
                case FLOAT->{float number=in.readFloat();if(!Float.isFinite(number))throw new DecoderException("WIGO non-finite float");yield new com.google.gson.JsonParser().parse(Float.toString(number));}
                case DOUBLE->{double number=in.readDouble();if(!Double.isFinite(number))throw new DecoderException("WIGO non-finite double");yield new com.google.gson.JsonParser().parse(Double.toString(number));}
                case DECIMAL->{String number=utf();if(number.length()>128||!number.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))throw new DecoderException("WIGO number");yield new com.google.gson.JsonParser().parse(number);}
                case STRING->new JsonPrimitive(utf());
                case UTF16_STRING->new JsonPrimitive(utf16());
                case INTEGER_STRING->{int value=in.readVarInt();if(value<0)throw new DecoderException("WIGO numeric key");yield new JsonPrimitive(Integer.toString(value));}
                case STATIC->{int id=in.readVarInt();if(id<0||id>=WireDictionary.VALUES.size())throw new DecoderException("WIGO static ID");yield new JsonPrimitive(WireDictionary.VALUES.get(id));}
                case UUID_VALUE->new JsonPrimitive(new UUID(in.readLong(),in.readLong()).toString());
                default->throw new DecoderException("WIGO value type");
            };
            if(scalar.isJsonPrimitive()&&scalar.getAsJsonPrimitive().isNumber()&&!Double.isFinite(scalar.getAsDouble()))throw new DecoderException("WIGO non-finite number");
            consume(scalar.toString().length());return scalar;
        }
        String utf() {
            int length=in.readVarInt();if(length<0||length>maxChars*3||length>in.readableBytes())throw new DecoderException("WIGO string length");
            byte[] bytes=new byte[length];in.readBytes(bytes);
            try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
            catch(CharacterCodingException invalid){throw new DecoderException("WIGO UTF-8",invalid);}
        }
        String utf16() {
            int length=in.readVarInt();if(length<0||(length&1)!=0||length>maxChars*2||length>in.readableBytes())throw new DecoderException("WIGO UTF-16 length");
            byte[] bytes=new byte[length];in.readBytes(bytes);
            try{return StandardCharsets.UTF_16LE.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
            catch(CharacterCodingException invalid){throw new DecoderException("WIGO UTF-16",invalid);}
        }
    }
}
