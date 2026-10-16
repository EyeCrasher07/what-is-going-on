package dev.wigo.config;

import dev.wigo.Wigo;
import dev.wigo.platform.Platform;
import net.minecraft.server.MinecraftServer;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;

/** Server-owned policy. Disk checks happen once per 100 server ticks, never per packet. */
public final class ServerMenus {
    private static MinecraftServer owner;
    private static MenuConfig config=MenuConfig.defaults();
    private static FileTime modified;
    private static long size=-1;
    private static int nextCheck;
    private static long revision;
    private ServerMenus() {}
    public static Path path() {return Platform.get().configDirectory().resolve(MenuConfig.FILE_NAME);}
    public static void tick(MinecraftServer server) {
        if(owner!=server) {owner=server;config=MenuConfig.defaults();modified=null;size=-1;nextCheck=server.getTickCount();revision++;}
        if(server.getTickCount()<nextCheck)return;
        nextCheck=server.getTickCount()+100;
        try {
            boolean created=MenuConfig.createIfMissing(path());
            if(modified==null) {
                try { MenuConfig.addNewMenus(path()); }
                catch(IllegalArgumentException invalid) { /* Keep invalid administrator edits intact. */ }
            }
            var attributes=Files.readAttributes(path(),java.nio.file.attribute.BasicFileAttributes.class);
            if(created||!attributes.lastModifiedTime().equals(modified)||attributes.size()!=size) {
                modified=attributes.lastModifiedTime();size=attributes.size();reload();
            }
        } catch(IOException failure) {Wigo.LOG.warn("Cannot access WIGO server menu config {}: {}",path(),failure.toString());}
    }
    public static void reload() {
        try {config=MenuConfig.read(path());revision++;Wigo.LOG.info("WIGO server menu rules loaded from {}",path());}
        catch(IOException|IllegalArgumentException failure) {Wigo.LOG.warn("Cannot load WIGO server menu config {}; keeping last valid rules: {}",path(),failure.getMessage());}
    }
    public static boolean allows(String id) {return config.allows(id);}
    public static boolean allowsUnclassified() {return config.allowsAll();}
    public static long revision() {return revision;}
    public static int maxMenuDistance(){return config.maxMenuDistance();}
    public static boolean allowPlayerDistance(){return config.allowPlayerDistance();}
    public static boolean countInvisibleObservers(){return config.countInvisibleObservers();}
    public static boolean countSpectatorObservers(){return config.countSpectatorObservers();}
    public static boolean allowObserverCounter(){return config.allowObserverCounter();}
    public static boolean allowSmoothMode(){return config.allowSmoothMode();}
    public static java.util.Set<String> blocked() {return config.blocked();}
    public static void clear() {owner=null;config=MenuConfig.defaults();modified=null;size=-1;nextCheck=0;}
}
