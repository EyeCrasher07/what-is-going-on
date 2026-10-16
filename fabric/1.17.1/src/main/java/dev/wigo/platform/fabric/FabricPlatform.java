package dev.wigo.platform.fabric;

import dev.wigo.platform.Platform;
import dev.wigo.protocol.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.Path;

public final class FabricPlatform implements Platform.Bindings {
    public boolean canSend(ServerPlayer player) {return ServerPlayNetworking.canSend(player,Wire.State.TYPE.id());}
    public void send(ServerPlayer player,Wire.State payload) {var connection=((dev.wigo.mixin.ServerConnectionAccessor)player.connection).wigoConnection();player.connection.send(LegacyWirePackets.packet(payload,LegacyWirePackets.threshold(connection),PacketFlow.CLIENTBOUND));}
    private static Connection connection() {var listener=Minecraft.getInstance().getConnection();return listener==null?null:listener.getConnection();}
    public boolean canSendActivity() {return connection()!=null&&ClientPlayNetworking.canSend(Wire.Activity.TYPE.id());}
    public boolean canSendGui() {return connection()!=null&&ClientPlayNetworking.canSend(Wire.Gui.TYPE.id());}
    public void send(Wire.Activity payload) {var connection=connection();connection.send(LegacyWirePackets.packet(payload,LegacyWirePackets.threshold(connection),PacketFlow.SERVERBOUND));}
    public void send(Wire.Gui payload) {var connection=connection();connection.send(LegacyWirePackets.packet(payload,LegacyWirePackets.threshold(connection),PacketFlow.SERVERBOUND));}
    public Path configDirectory() {return FabricLoader.getInstance().getConfigDir();}
    public boolean isModLoaded(String id) {return FabricLoader.getInstance().isModLoaded(id);}
}
