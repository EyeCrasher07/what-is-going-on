package dev.wigo.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.FilterMode;
import dev.wigo.config.SharingRules;
import dev.wigo.config.CursorStyle;
import dev.wigo.mixin.RenderTypeInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import java.util.List;
import java.util.UUID;

/** Same cylindrical mesh, brightness shader and local textures as the real observer panels. */
public final class SettingsPreviewRenderer extends PictureInPictureRenderer<SettingsPreviewState> {
    private static final int PAD=16, WIDTH=208, HEIGHT=199, SCALE=3;
    private final Identifier textureId=Identifier.fromNamespaceAndPath("wigo","preview/"+UUID.randomUUID());
    private static final Identifier CHEST=Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private TextureTarget target;
    private GuiRenderer menuRenderer;
    private GuiRenderState menuState;
    private RenderType panelType;
    private static int resourceVersion;
    private int renderedVersion=-1;
    private CursorStyle renderedCursor;
    private String renderedLanguage;
    public static void invalidate(){resourceVersion++;}

    @Override public Class<SettingsPreviewState> getRenderStateClass(){return SettingsPreviewState.class;}
    @Override protected String getTextureLabel(){return "WIGO settings preview";}
    @Override protected float getTranslateY(int height,int guiScale){return height*.90f;}

    @Override public void prepare(SettingsPreviewState state,GuiRenderState gui,FeatureRenderDispatcher features,int scale) {
        var mc=Minecraft.getInstance();
        if(target==null) {
            target=new TextureTarget("WIGO preview menu",WIDTH*SCALE,HEIGHT*SCALE,GpuFormat.RGBA8_UNORM,GpuFormat.D32_FLOAT);
            mc.getTextureManager().register(textureId,new PreviewTexture(target));
            menuState=new GuiRenderState();
            menuRenderer=GuiRendererFactory.create(menuState,mc,List.of());
            var pipeline=RenderPipeline.builder()
                    .withLocation(Identifier.fromNamespaceAndPath("wigo","pipeline/preview"))
                    .withBindGroupLayout(BindGroupLayouts.GLOBALS).withBindGroupLayout(BindGroupLayouts.PROJECTION)
                    .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS).withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                    .withVertexShader("core/position_tex_color").withFragmentShader(Identifier.fromNamespaceAndPath("wigo","core/menu"))
                    .withVertexBinding(0,DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withCull(true).withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)).build();
            panelType=RenderTypeInvoker.wigoCreate("wigo_preview",RenderSetup.builder(pipeline).withTexture("Sampler0",textureId).createRenderSetup());
        }
        if(renderedVersion!=resourceVersion||renderedCursor!=state.cursor()||!mc.options.languageCode.equals(renderedLanguage))renderMenu(state,mc);
        super.prepare(state,gui,features,scale);
    }

    private void renderMenu(SettingsPreviewState state,Minecraft mc) {
        if(renderedVersion!=-1&&renderedVersion!=resourceVersion) {
            menuRenderer.close();menuState=new GuiRenderState();menuRenderer=GuiRendererFactory.create(menuState,mc,List.of());
        }
        var projection=RenderSystem.getProjectionMatrixBuffer();var projectionType=RenderSystem.getProjectionType();
        try {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                    target.getColorTexture(),new Vector4f(0,0,0,0),target.getDepthTexture(),0);
            ((OffscreenGui)menuRenderer).wigoTarget(target,SCALE);
            try(var viewport=GuiViewport.open(WIDTH,HEIGHT)) {
                var g=new GuiGraphicsExtractor(mc,menuState,-100,-100);
                g.blit(RenderPipelines.GUI_TEXTURED,CHEST,PAD,PAD,0,0,176,71,256,256);
                g.blit(RenderPipelines.GUI_TEXTURED,CHEST,PAD,PAD+71,0,126,176,96,256,256);
                g.text(mc.font,Component.translatable("container.chest"),PAD+8,PAD+6,0xFF404040,false);
                g.text(mc.font,Component.translatable("container.inventory"),PAD+8,PAD+73,0xFF404040,false);
                ItemStack[] items={new ItemStack(Items.DIAMOND,64),new ItemStack(Items.APPLE,12),new ItemStack(Items.ENDER_PEARL,8)};
                for(int i=0;i<items.length;i++) {
                    int x=PAD+8+i*18;
                    g.item(items[i],x,PAD+18);g.itemDecorations(mc.font,items[i],x,PAD+18);
                }
                g.nextStratum();CursorSprites.draw(g,state.cursor(),PAD+65,PAD+45);
                menuRenderer.render();menuRenderer.endFrame();
                renderedVersion=resourceVersion;renderedCursor=state.cursor();renderedLanguage=mc.options.languageCode;
            }
        } finally {
            ((OffscreenGui)menuRenderer).wigoTarget(null,SCALE);
            if(projection!=null)RenderSystem.setProjectionMatrix(projection,projectionType);
        }
    }

    @Override protected void renderToTexture(SettingsPreviewState state,PoseStack pose,SubmitNodeCollector collector) {
        var mc=Minecraft.getInstance();
        mc.gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        float yaw=(float)Math.toRadians(state.yaw()),elevation=(float)Math.toRadians(state.elevation());
        pose.rotate(new Quaternionf().rotationZ((float)Math.PI).rotateX(elevation).rotateY(yaw));
        pose.translate(0,-.1f,0);
        var camera=new CameraRenderState();
        camera.orientation=new Quaternionf().rotationY(-yaw);
        mc.getEntityRenderDispatcher().submit(state.player(),camera,0,0,0,pose,collector);
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
        pose.rotate(new Quaternionf().rotationX((float)pitch));
        collector.submitCustomGeometry(pose,panelType,(matrix,buffer)->{
            for(var v:vertices)buffer.addVertex(matrix,v.x(),v.y(),v.z()).setUv(v.u(),v.v()).setColor(color);
        });
        pose.popPose();
    }

    @Override public void close() {
        super.close();
        if(menuRenderer!=null){menuRenderer.close();menuRenderer=null;menuState=null;}
        if(target!=null){Minecraft.getInstance().getTextureManager().release(textureId);target=null;renderedVersion=-1;}
    }
    private static final class PreviewTexture extends AbstractTexture {
        private final TextureTarget target;
        PreviewTexture(TextureTarget target) {
            this.target=target;texture=target.getColorTexture();textureView=target.getColorTextureView();
            sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        }
        @Override public void close(){target.destroyBuffers();texture=null;textureView=null;}
    }
}
