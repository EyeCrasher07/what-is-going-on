package dev.wigo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import java.util.List;
import java.util.function.Supplier;

public final class GuiRendererFactory {
    private static Factory factory;
    private GuiRendererFactory() {}
    public static void install(Factory value) { factory=value; }
    public static GuiRenderer create(GuiRenderState state,Minecraft mc,List<Supplier<PictureInPictureRenderer<?>>> factories) {
        if(factory==null) throw new IllegalStateException("WIGO client renderer not initialized");
        return factory.create(state,mc,factories);
    }
    @FunctionalInterface public interface Factory {
        GuiRenderer create(GuiRenderState state,Minecraft mc,List<Supplier<PictureInPictureRenderer<?>>> factories);
    }
}
