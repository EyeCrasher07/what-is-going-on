package dev.wigo.platform.fabric;
import dev.wigo.protocol.Wire;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import dev.wigo.protocol.LegacyPayload;
public final class LegacyFabricPackets {
    public static final PacketType<Wrapped> ACTIVITY=PacketType.create(Wire.Activity.TYPE.id(),buf->wrap(Wire.Activity.CODEC.decode(buf)));
    public static final PacketType<Wrapped> GUI=PacketType.create(Wire.Gui.TYPE.id(),buf->wrap(Wire.Gui.CODEC.decode(buf)));
    public static final PacketType<Wrapped> STATE=PacketType.create(Wire.State.TYPE.id(),buf->wrap(Wire.State.CODEC.decode(buf)));
    public static Wrapped wrap(LegacyPayload payload) {
        var type=payload instanceof Wire.Activity?ACTIVITY:payload instanceof Wire.Gui?GUI:STATE;
        return new Wrapped(payload,type);
    }
    public record Wrapped(LegacyPayload payload,PacketType<Wrapped> type) implements FabricPacket {
        public void write(FriendlyByteBuf buf) {payload.write(buf);}
        public PacketType<?> getType() {return type;}
    }
}
