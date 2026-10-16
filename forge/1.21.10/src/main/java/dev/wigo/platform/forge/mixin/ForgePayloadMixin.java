package dev.wigo.platform.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wigo.protocol.NetworkSessions;
import dev.wigo.protocol.Wire;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.NetworkInstance;
import org.spongepowered.asm.mixin.Mixin;

/** Forge defers its payload codec until dispatch, beyond the native packet decoder. */
@Mixin(value=NetworkInstance.class,remap=false)
public abstract class ForgePayloadMixin {
    @WrapMethod(method="dispatch",remap=false)
    private boolean wigoPayload(CustomPayloadEvent event,Operation<Boolean> original) {
        var id=event.getChannel();
        if(!id.equals(Wire.State.TYPE.id())&&!id.equals(Wire.Activity.TYPE.id())&&!id.equals(Wire.Gui.TYPE.id()))return original.call(event);
        try(var scope=NetworkSessions.scope(NetworkSessions.peer(event.getSource().getConnection()))) {return original.call(event);}
    }
}
