package dev.wigo.platform.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class NeoGuiRendererFactory {
    private NeoGuiRendererFactory() {}
    @SuppressWarnings({"rawtypes","unchecked"})
    public static GuiRenderer create(GuiRenderState state,Minecraft mc,List<Supplier<PictureInPictureRenderer<?>>> factories) {
        List<PictureInPictureRendererRegistration<?>> registrations=new ArrayList<>();
        for(var factory:factories) registrations.add(new PictureInPictureRendererRegistration(factory.get().getRenderStateClass(),buffers -> factory.get()));
        return new GuiRenderer(state,mc.renderBuffers().bufferSource(),mc.gameRenderer.getSubmitNodeStorage(),mc.gameRenderer.getFeatureRenderDispatcher(),registrations);
    }
}
