package dev.wigo.client;

import java.io.IOException;
import java.util.Properties;

/** Values expanded from the same Gradle version as the loader metadata. */
public final class BuildInfo {
    private static final Properties values=new Properties();
    static {
        try(var stream=BuildInfo.class.getResourceAsStream("/assets/wigo/build.properties")) {
            if(stream!=null)values.load(stream);
        }catch(IOException ignored){}
    }
    private BuildInfo(){}
    public static String version(){return values.getProperty("version","unknown");}
    public static String loader(){return values.getProperty("loader","unknown");}
}
