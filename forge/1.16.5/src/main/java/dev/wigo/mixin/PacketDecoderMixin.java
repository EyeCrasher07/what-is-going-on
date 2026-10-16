package dev.wigo.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wigo.protocol.NetworkSessions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NettyPacketDecoder;
import org.spongepowered.asm.mixin.Mixin;
import java.util.List;
@Mixin(NettyPacketDecoder.class)
public abstract class PacketDecoderMixin {
    @WrapMethod(method="decode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V")
    private void wigoDecode(ChannelHandlerContext context,ByteBuf input,List<Object> output,Operation<Void> original) {
        try(var scope=NetworkSessions.scope(NetworkSessions.peer(context.channel()))) {original.call(context,input,output);}
    }
}
