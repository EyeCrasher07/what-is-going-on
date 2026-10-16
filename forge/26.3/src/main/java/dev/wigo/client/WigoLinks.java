package dev.wigo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import java.net.URI;
import java.util.Map;
import java.util.WeakHashMap;

/** Vanilla browser confirmation stays within the owner's opt-in F8 privacy rule. */
public final class WigoLinks {
    private static final Map<Screen,WigoScreen> confirmations=new WeakHashMap<>();
    private WigoLinks(){}
    public static void open(WigoScreen previous,URI uri){
        ConfirmLinkScreen.confirmLinkNow(previous,uri);
        confirmations.put(Minecraft.getInstance().gui.screen(),previous);
    }
    public static boolean owns(Screen screen){return screen instanceof WigoScreen||confirmations.containsKey(screen);}
    public static Screen previous(Screen screen){return screen instanceof WigoScreen own?own.previousScreen():confirmations.get(screen).previousScreen();}
}
