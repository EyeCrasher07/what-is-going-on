package dev.wigo.platform.neoforge;
import dev.wigo.platform.Platform;
import dev.wigo.protocol.Wire;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import java.nio.file.Path;
public final class NeoForgePlatform implements Platform.Bindings {
    public boolean canSend(ServerPlayer player) {return NetworkRegistry.getInstance().isConnected(player.connection,Wire.State.TYPE.id());}
    public void send(ServerPlayer player,Wire.State payload) {PacketDistributor.PLAYER.with(player).send(payload);}
    public boolean canSendActivity() {var connection=Minecraft.getInstance().getConnection();return connection!=null&&NetworkRegistry.getInstance().isConnected(connection,Wire.Activity.TYPE.id());}
    public boolean canSendGui() {var connection=Minecraft.getInstance().getConnection();return connection!=null&&NetworkRegistry.getInstance().isConnected(connection,Wire.Gui.TYPE.id());}
    public void send(Wire.Activity payload) {PacketDistributor.SERVER.noArg().send(payload);}
    public void send(Wire.Gui payload) {PacketDistributor.SERVER.noArg().send(payload);}
    public Path configDirectory() {return FMLPaths.CONFIGDIR.get();}
    public boolean isModLoaded(String id) {return ModList.get().isLoaded(id);}
}
