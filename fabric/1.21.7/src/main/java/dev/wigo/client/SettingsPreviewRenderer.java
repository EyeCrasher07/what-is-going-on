package dev.wigo.client;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.fog.FogRenderer;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.textures.FilterMode;
import dev.wigo.mixin.RenderTypeInvoker;
import dev.wigo.Wigo;
import dev.wigo.config.SharingRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.*;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.*;


import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import org.joml.Quaternionf;
import com.mojang.blaze3d.platform.Lighting;

public final class SettingsPreviewRenderer extends PictureInPictureRenderer<SettingsPreviewState> {
    private static final int WIDTH=208,HEIGHT=199,SCALE=3;
    private final ResourceLocation id=ResourceLocation.fromNamespaceAndPath("wigo","preview/"+UUID.randomUUID());
    private TextureTarget target;
    private GuiRenderer menuRenderer;
    private GuiRenderState menuState;
    private RenderType panelType;
    private net.minecraft.client.renderer.fog.FogRenderer uiFog;
    private RemoteMenu sample;
    private static int resourceVersion;
    private int rendered=-1;
    private dev.wigo.config.CursorStyle cursor;
    private String language;
    public static void invalidate(){resourceVersion++;}
    public SettingsPreviewRenderer(){super(Minecraft.getInstance().renderBuffers().bufferSource());}
    @Override public Class<SettingsPreviewState> getRenderStateClass(){return SettingsPreviewState.class;}
    @Override protected String getTextureLabel(){return "WIGO settings preview";}
    @Override protected float getTranslateY(int height,int scale){return height*.90f;}
    @Override public void prepare(SettingsPreviewState state,GuiRenderState gui,int scale) {
        var mc=Minecraft.getInstance();
        if(target==null) {
            target = new TextureTarget("WIGO local menu", WIDTH*SCALE, HEIGHT*SCALE,
                    true);
            Minecraft.getInstance().getTextureManager().register(id, new PreviewTexture(target));
            panelType=RenderTypeInvoker.wigoCreate("wigo_menu",1536,PanelRenderer.pipeline(true),RenderType.CompositeState.builder().setTextureState(new RenderStateShard.TextureStateShard(id,false)).createCompositeState(false));

            menuState=new GuiRenderState();menuRenderer=GuiRendererFactory.create(menuState,mc,List.of());uiFog=new net.minecraft.client.renderer.fog.FogRenderer();
            sample=new RemoteMenu(UUID.randomUUID());sample.state.kind="chest";sample.state.rows=3;sample.state.slotCount=63;
            sample.items=new ItemStack[63];Arrays.fill(sample.items,ItemStack.EMPTY);
            sample.items[0]=new ItemStack(net.minecraft.world.item.Items.DIAMOND,64);
            sample.items[1]=new ItemStack(net.minecraft.world.item.Items.APPLE,12);
            sample.items[2]=new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL,8);
        }
        if(rendered!=resourceVersion||cursor!=state.cursor()||!mc.options.languageCode.equals(language))renderMenu(state,mc);
        super.prepare(state,gui,scale);
    }
    private void renderMenu(SettingsPreviewState state,Minecraft mc) {
        var projection=RenderSystem.getProjectionMatrixBuffer();var projectionType=RenderSystem.getProjectionType();
        try {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                        target.getColorTexture(), 0, target.getDepthTexture(), 1);
            ((OffscreenGui)menuRenderer).wigoTarget(target,SCALE);
            try(var viewport=GuiViewport.open(WIDTH,HEIGHT)) {
                var g=new GuiGraphics(mc,menuState);
                PanelRenderer.draw(g,sample,mc.player,null);
                g.nextStratum();CursorSprites.draw(g,state.cursor(),81,61);
                menuRenderer.render(uiFog.getBuffer(FogRenderer.FogMode.NONE));menuRenderer.incrementFrameNumber(); uiFog.endFrame();
                rendered=resourceVersion;cursor=state.cursor();language=mc.options.languageCode;
            }
        } finally {
            ((OffscreenGui)menuRenderer).wigoTarget(null,SCALE);
            if(projection!=null)RenderSystem.setProjectionMatrix(projection,projectionType);
        }
    }
    @Override protected void renderToTexture(SettingsPreviewState state,PoseStack pose) {
        var mc=Minecraft.getInstance();
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        float yaw=(float)Math.toRadians(state.yaw()),elevation=(float)Math.toRadians(state.elevation());
        pose.mulPose(new Quaternionf().rotationZ((float)Math.PI).rotateX(elevation).rotateY(yaw));
        pose.translate(0,-.1f,0);
        var buffers=mc.renderBuffers().bufferSource();var dispatcher=mc.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);try{dispatcher.render(state.player(),0,0,0,pose,buffers,15728880);buffers.endBatch();}finally{dispatcher.setRenderShadow(true);}
        if(state.transparency()>=100)return;
        float unit=1.6f/176*state.size()/100f;
        double pitch=Math.toRadians(state.pitch());
        var shape=PanelShape.mesh(WIDTH*unit/2,HEIGHT*unit/2,state.curvature(),state.mirror(),false,state.reverse());
        double lowest=Double.POSITIVE_INFINITY;
        for(var v:shape)lowest=Math.min(lowest,v.y()*Math.cos(pitch)-v.z()*Math.sin(pitch));
        double height=Math.max(1.55+state.offset()/100.0,.025-lowest);
        // This GUI camera is orthographic: use its direction, not a finite world position.
        double viewX=Math.sin(yaw)*Math.cos(elevation),viewY=-Math.sin(elevation),viewZ=-Math.cos(yaw)*Math.cos(elevation);
        boolean back=PanelShape.backView(viewX,viewY,viewZ,0,state.pitch());
        shape=PanelShape.mesh(WIDTH*unit/2,HEIGHT*unit/2,state.curvature(),state.mirror(),back,state.reverse());
        var vertices=shape;
        int color=PanelShape.vertexColor(Math.round(255*(1-state.transparency()/100f)),state.brightness());
        pose.pushPose();
        pose.translate(0,height,SharingRules.PANEL_FORWARD_OFFSET);
        pose.mulPose(new Quaternionf().rotationX((float)pitch));
        var buffer=buffers.getBuffer(panelType);
        for(var vertex:vertices)buffer.addVertex(pose.last(),vertex.x(),vertex.y(),vertex.z()).setUv(vertex.u(),vertex.v()).setColor(color);
        buffers.endBatch(panelType);
        pose.popPose();
    }
    @Override public void close() {
        super.close();if(menuRenderer!=null){menuRenderer.close();menuRenderer=null;menuState=null;}if(uiFog!=null){uiFog.close();uiFog=null;}
        if(target!=null){Minecraft.getInstance().getTextureManager().release(id);target=null;rendered=-1;}
    }
    private static final class PreviewTexture extends AbstractTexture {
        private final TextureTarget target;
        PreviewTexture(TextureTarget target) {
            this.target = target; texture = target.getColorTexture(); textureView = target.getColorTextureView();
            texture.setAddressMode(com.mojang.blaze3d.textures.AddressMode.CLAMP_TO_EDGE);
            texture.setTextureFilter(FilterMode.NEAREST,false);
        }
        @Override public void close() { target.destroyBuffers(); texture = null; textureView = null; }
    }
}
