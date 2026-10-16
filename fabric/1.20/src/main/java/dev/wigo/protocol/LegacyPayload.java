package dev.wigo.protocol;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public interface LegacyPayload {
    ResourceLocation id();
    void write(FriendlyByteBuf buffer);
}
