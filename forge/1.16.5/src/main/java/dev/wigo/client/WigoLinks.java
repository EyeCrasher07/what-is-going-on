package dev.wigo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ConfirmOpenLinkScreen;
import java.net.URI;
import java.util.Map;
import java.util.WeakHashMap;

/** Vanilla browser confirmation stays within the owner's opt-in F8 privacy rule. */
public final class WigoLinks {
    private static final Map<Screen,WigoScreen> confirmations=new WeakHashMap<>();
    private WigoLinks(){}
    public static void open(WigoScreen previous,URI uri){
        net.minecraft.client.Minecraft.getInstance().setScreen(new ConfirmOpenLinkScreen(accepted->{
            if(accepted)net.minecraft.util.Util.getPlatform().openUri(uri);net.minecraft.client.Minecraft.getInstance().setScreen(previous);
        },uri.toString(),true));
        confirmations.put(Minecraft.getInstance().screen,previous);
    }
    public static boolean owns(Screen screen){return screen instanceof WigoScreen||confirmations.containsKey(screen);}
    public static Screen previous(Screen screen){return screen instanceof WigoScreen own?own.previousScreen():confirmations.get(screen).previousScreen();}
}
