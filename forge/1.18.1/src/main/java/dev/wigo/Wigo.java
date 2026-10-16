package dev.wigo;

import dev.wigo.protocol.Wire;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public final class Wigo {
    public static final Logger LOG = LogManager.getLogger("WIGO");
    private Wigo() {}
    public static void initialize() {
        LOG.info("WIGO state protocol v{} ready; adaptive zlib compression; no image streaming",Wire.VERSION);
    }
}
