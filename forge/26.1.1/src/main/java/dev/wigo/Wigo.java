package dev.wigo;

import dev.wigo.protocol.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Wigo {
    public static final Logger LOG = LoggerFactory.getLogger("WIGO");
    private Wigo() {}
    public static void initialize() {
        LOG.info("WIGO state protocol v{} ready; adaptive zlib compression; no image streaming",Wire.VERSION);
    }
}
