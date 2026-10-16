package dev.wigo.mixin;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.ServerPlayNetHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayNetHandler.class)
public interface ServerConnectionAccessor {
    @Accessor("connection") NetworkManager wigoConnection();
}
