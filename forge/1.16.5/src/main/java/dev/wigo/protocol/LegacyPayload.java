package dev.wigo.protocol;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public interface LegacyPayload {
    ResourceLocation id();
    void write(PacketBuffer buffer);
}
