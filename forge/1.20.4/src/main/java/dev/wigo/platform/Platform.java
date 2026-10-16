package dev.wigo.platform;

import dev.wigo.protocol.Wire;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.Path;

/** Loader services used by the menu code; each distribution installs one implementation. */
public final class Platform {
    private static Bindings bindings;
    private Platform() {}
    public static void install(Bindings value) { bindings=value; }
    public static Bindings get() {
        if(bindings==null) throw new IllegalStateException("WIGO platform not initialized");
        return bindings;
    }
    public interface Bindings {
        boolean canSend(ServerPlayer player);
        void send(ServerPlayer player,Wire.State payload);
        boolean canSendActivity();
        boolean canSendGui();
        void send(Wire.Activity payload);
        void send(Wire.Gui payload);
        Path configDirectory();
        boolean isModLoaded(String id);
    }
}
