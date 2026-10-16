package dev.wigo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import java.util.List;
import java.util.function.Supplier;

public final class GuiRendererFactory {
    private static Factory factory;
    private static boolean creatingOffscreen;
    public static boolean creatingOffscreen() { return creatingOffscreen; }
    private GuiRendererFactory() {}
    public static void install(Factory value) { factory=value; }
    public static GuiRenderer create(GuiRenderState state,Minecraft mc,List<Supplier<PictureInPictureRenderer<?>>> factories) {
        if(factory==null) throw new IllegalStateException("WIGO client renderer not initialized");
        creatingOffscreen=true;
        try { return factory.create(state,mc,factories); } finally { creatingOffscreen=false; }
    }
    @FunctionalInterface public interface Factory {
        GuiRenderer create(GuiRenderState state,Minecraft mc,List<Supplier<PictureInPictureRenderer<?>>> factories);
    }
}
