package dev.wigo.platform.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class NeoGuiRendererFactory {
    private NeoGuiRendererFactory() {}
    @SuppressWarnings({"rawtypes","unchecked"})
    public static GuiRenderer create(GuiRenderState state,Minecraft mc,List<Supplier<PictureInPictureRenderer<?>>> factories) {
        List<PictureInPictureRendererRegistration<?>> registrations=new ArrayList<>();
        for(var factory:factories) registrations.add(new PictureInPictureRendererRegistration(factory.get().getRenderStateClass(),factory));
        return new GuiRenderer(state,mc.gameRenderer.featureRenderDispatcher(),registrations);
    }
}
