package dev.wigo.platform.fabric;

import dev.wigo.platform.Platform;
import dev.wigo.protocol.Wire;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.Path;

public final class FabricPlatform implements Platform.Bindings {
    public boolean canSend(ServerPlayer player) {return ServerPlayNetworking.canSend(player,Wire.State.TYPE.id());}
    public void send(ServerPlayer player,Wire.State payload) {ServerPlayNetworking.send(player,LegacyFabricPackets.wrap(payload));}
    public boolean canSendActivity() {return ClientPlayNetworking.canSend(Wire.Activity.TYPE.id());}
    public boolean canSendGui() {return ClientPlayNetworking.canSend(Wire.Gui.TYPE.id());}
    public void send(Wire.Activity payload) {ClientPlayNetworking.send(LegacyFabricPackets.wrap(payload));}
    public void send(Wire.Gui payload) {ClientPlayNetworking.send(LegacyFabricPackets.wrap(payload));}
    public Path configDirectory() {return FabricLoader.getInstance().getConfigDir();}
    public boolean isModLoaded(String id) {return FabricLoader.getInstance().isModLoaded(id);}
}
