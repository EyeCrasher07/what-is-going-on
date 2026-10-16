package dev.wigo.protocol;

import net.minecraft.network.FriendlyByteBuf;
import dev.wigo.protocol.LegacyPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** The same bounded v4 frames, carried by the older game's id/write payload API. */
public final class Wire {
    public static final int VERSION=4,MAX_TEXT=16384;
    private Wire() {}
    public record Type<T>(ResourceLocation id) {}
    public record Codec<T>(BiConsumer<FriendlyByteBuf,T> writer,Function<FriendlyByteBuf,T> reader) {
        public void encode(FriendlyByteBuf buf,T value) {writer.accept(buf,value);}
        public T decode(FriendlyByteBuf buf) {return reader.apply(buf);}
    }
    public record Activity(CompressedText.Payload payload) implements LegacyPayload {
        public Activity(String json) {this(new CompressedText.Payload(json,1024));}
        public String json() {return payload.json();}
        public static final Type<Activity> TYPE=new Type<>(new ResourceLocation("wigo","activity_v4"));
        public static final Codec<Activity> CODEC=new Codec<>((buf,value)->value.write(buf),buf->new Activity(CompressedText.readSafely(buf,1024)));
        public void write(FriendlyByteBuf buf) {payload.write(buf);}
        public ResourceLocation id() {return TYPE.id();}
    }
    public record State(CompressedText.Payload payload) implements LegacyPayload {
        public State(String json) {this(new CompressedText.Payload(json,MAX_TEXT));}
        public static State legacy(String json) {return new State(new CompressedText.Payload(json,MAX_TEXT).legacy());}
        public String json() {return payload.json();}
        public static final Type<State> TYPE=new Type<>(new ResourceLocation("wigo","menu_v4"));
        public static final Codec<State> CODEC=new Codec<>((buf,value)->value.write(buf),buf->new State(CompressedText.readSafely(buf,MAX_TEXT)));
        public void write(FriendlyByteBuf buf) {payload.write(buf);}
        public ResourceLocation id() {return TYPE.id();}
    }
    public record Gui(CompressedText.Payload payload) implements LegacyPayload {
        public Gui(String json) {this(new CompressedText.Payload(json,MAX_TEXT));}
        public static Gui legacy(String json) {return new Gui(new CompressedText.Payload(json,MAX_TEXT).legacy());}
        public String json() {return payload.json();}
        public static final Type<Gui> TYPE=new Type<>(new ResourceLocation("wigo","gui_v4"));
        public static final Codec<Gui> CODEC=new Codec<>((buf,value)->value.write(buf),buf->new Gui(CompressedText.readSafely(buf,MAX_TEXT)));
        public void write(FriendlyByteBuf buf) {payload.write(buf);}
        public ResourceLocation id() {return TYPE.id();}
    }
}
