package dev.wigo.platform.forge;
import dev.wigo.platform.Platform;
import dev.wigo.protocol.Wire;
import net.minecraft.client.Minecraft;
import net.minecraft.network.*;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.game.*;
import dev.wigo.protocol.LegacyPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.minecraftforge.fmllegacy.network.event.EventNetworkChannel;
import io.netty.buffer.Unpooled;
import java.nio.file.Path;
import java.util.function.Consumer;
public final class ForgePlatform implements Platform.Bindings {
    private static Consumer<String> receiver=text->{};
    private static EventNetworkChannel activity,gui,state;
    private static EventNetworkChannel channel(net.minecraft.resources.ResourceLocation id) {
        var version=Integer.toString(Wire.VERSION);var accepts=NetworkRegistry.acceptMissingOr(version);
        return NetworkRegistry.newEventChannel(id,()->version,accepts,accepts);
    }
    public static void initialize() {
        activity=channel(Wire.Activity.TYPE.id());gui=channel(Wire.Gui.TYPE.id());state=channel(Wire.State.TYPE.id());
        activity.addListener((NetworkEvent.ClientCustomPayloadEvent event)->{
            var context=event.getSource().get();var text=Wire.Activity.CODEC.decode(event.getPayload()).json();context.setPacketHandled(true);
            if(context.getSender()!=null)context.enqueueWork(()->dev.wigo.WigoServer.activity(context.getSender(),text));
        });
        gui.addListener((NetworkEvent.ClientCustomPayloadEvent event)->{
            var context=event.getSource().get();var text=Wire.Gui.CODEC.decode(event.getPayload()).json();context.setPacketHandled(true);
            if(context.getSender()!=null)context.enqueueWork(()->dev.wigo.WigoServer.gui(context.getSender(),text));
        });
        state.addListener((NetworkEvent.ServerCustomPayloadEvent event)->{
            var context=event.getSource().get();var text=Wire.State.CODEC.decode(event.getPayload()).json();context.setPacketHandled(true);
            context.enqueueWork(()->receiver.accept(text));
        });
    }
    public static void clientReceiver(Consumer<String> value) {receiver=value;}
    private static Connection connection() {var listener=Minecraft.getInstance().getConnection();return listener==null?null:listener.getConnection();}
    public boolean canSend(ServerPlayer player) {return state.isRemotePresent(player.connection.connection);}
    public void send(ServerPlayer player,Wire.State payload) {var connection=player.connection.connection;player.connection.send(packet(payload,threshold(connection),PacketFlow.CLIENTBOUND));}
    public boolean canSendActivity() {var connection=connection();return connection!=null&&activity.isRemotePresent(connection);}
    public boolean canSendGui() {var connection=connection();return connection!=null&&gui.isRemotePresent(connection);}
    public void send(Wire.Activity payload) {var connection=connection();connection.send(packet(payload,threshold(connection),PacketFlow.SERVERBOUND));}
    public void send(Wire.Gui payload) {var connection=connection();connection.send(packet(payload,threshold(connection),PacketFlow.SERVERBOUND));}
    private static int threshold(Connection connection) {var encoder=connection.channel().pipeline().get(CompressionEncoder.class);return encoder==null?-1:encoder.getThreshold();}
    public static Packet<?> packet(LegacyPayload payload,int threshold,PacketFlow flow) {
        return dev.wigo.protocol.LegacyWirePackets.packet(payload,threshold,flow);
    }
    public Path configDirectory() {return FMLPaths.CONFIGDIR.get();}
    public boolean isModLoaded(String id) {return ModList.get().isLoaded(id);}
}
