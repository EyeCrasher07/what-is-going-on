package dev.wigo.protocol;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class Wire {
    public static final int VERSION = 4;
    public static final int MAX_TEXT = 16384;
    private Wire() {}

    public record Activity(CompressedText.Payload payload) implements CustomPacketPayload {
        public Activity(String json) {this(new CompressedText.Payload(json,1024));}
        public String json() {return payload.json();}
        public static final Type<Activity> TYPE = new Type<>(Identifier.fromNamespaceAndPath("wigo", "activity_v4"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Activity> CODEC = StreamCodec.of(
                (buf, value) -> value.payload.write(buf), buf -> new Activity(CompressedText.readSafely(buf,1024)));
        @Override public Type<Activity> type() { return TYPE; }
    }
    public record State(CompressedText.Payload payload) implements CustomPacketPayload {
        public State(String json) {this(new CompressedText.Payload(json,MAX_TEXT));}
        public static State legacy(String json){return new State(new CompressedText.Payload(json,MAX_TEXT).legacy());}
        public String json() {return payload.json();}
        public static final Type<State> TYPE = new Type<>(Identifier.fromNamespaceAndPath("wigo", "menu_v4"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of(
                (buf, value) -> value.payload.write(buf), buf -> new State(CompressedText.readSafely(buf,MAX_TEXT)));
        @Override public Type<State> type() { return TYPE; }
    }
    public record Gui(CompressedText.Payload payload) implements CustomPacketPayload {
        public Gui(String json) {this(new CompressedText.Payload(json,MAX_TEXT));}
        public static Gui legacy(String json){return new Gui(new CompressedText.Payload(json,MAX_TEXT).legacy());}
        public String json() {return payload.json();}
        public static final Type<Gui> TYPE = new Type<>(Identifier.fromNamespaceAndPath("wigo", "gui_v4"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Gui> CODEC = StreamCodec.of(
                (buf, value) -> value.payload.write(buf), buf -> new Gui(CompressedText.readSafely(buf,MAX_TEXT)));
        @Override public Type<Gui> type() { return TYPE; }
    }
}
