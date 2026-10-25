package dev.wigo.client;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.pipeline.*;
import dev.wigo.mixin.RenderTypeInvoker;
import dev.wigo.Wigo;
import dev.wigo.config.SharingRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.*;


/** Local sample rendered through this version's existing native panel backend. */
public final class SettingsPreviewRenderer {
    private static final int WIDTH=208,HEIGHT=199,SCALE=3;
    private static final java.util.UUID OWNER=java.util.UUID.randomUUID();
    private static final net.minecraft.resources.ResourceLocation id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("wigo","preview/"+OWNER);
    private static TextureTarget target;
    private static RenderType type;

    private static RemoteMenu sample;
    private static net.minecraft.client.player.RemotePlayer mannequin;
    private static int version,rendered=-1;
    private static dev.wigo.config.CursorStyle cursor;
    private static String language;
    public static void invalidate(){version++;}
    public static net.minecraft.world.entity.LivingEntity player() {
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null)return null;
        if(mannequin==null||mannequin.level()!=mc.level)mannequin=new PreviewPlayer(mc.level,mc.player.getGameProfile());
        return mannequin;
    }
    private static final class PreviewPlayer extends net.minecraft.client.player.RemotePlayer {
        PreviewPlayer(net.minecraft.client.multiplayer.ClientLevel level,com.mojang.authlib.GameProfile profile){super(level,profile);}
        @Override public boolean isSpectator(){return false;}
        @Override public boolean isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart part) {
            var owner=Minecraft.getInstance().player;return owner!=null&&owner.isModelPartShown(part);
        }
    }
    public static void render(net.minecraft.client.gui.GuiGraphics g,SettingsPreviewState state) {
        var mc=Minecraft.getInstance();g.flush();
        if(target==null) {
            target = GuiScratch.create(WIDTH*SCALE, HEIGHT*SCALE);
            Minecraft.getInstance().getTextureManager().register(id, new PreviewTexture(target));
            type = LegacyPanelType.create(id,true);

            sample=new RemoteMenu(OWNER);sample.state.kind="chest";sample.state.rows=3;sample.state.slotCount=63;
            sample.items=new net.minecraft.world.item.ItemStack[63];java.util.Arrays.fill(sample.items,net.minecraft.world.item.ItemStack.EMPTY);
            sample.items[0]=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,64);
            sample.items[1]=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE,12);
            sample.items[2]=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL,8);
        }
        if(rendered!=version||cursor!=state.cursor()||!mc.options.languageCode.equals(language)) {
            try(var scratch=GuiScratch.open(target,SCALE);var viewport=GuiViewport.open(WIDTH,HEIGHT)) {
                GuiScratch.clear(target);
                var menu=new net.minecraft.client.gui.GuiGraphics(mc,mc.renderBuffers().bufferSource());
                PanelRenderer.draw(menu,sample,mc.player,null);
                CursorSprites.draw(menu,state.cursor(),81,61);menu.flush();
            }
            rendered=version;cursor=state.cursor();language=mc.options.languageCode;
        }
        float yaw=(float)Math.toRadians(state.yaw()),elevation=(float)Math.toRadians(state.elevation());
        float cx=(state.x0()+state.x1())/2f,cy=state.y0()+(state.y1()-state.y0())*.90f;
        g.enableScissor(state.x0(),state.y0(),state.x1(),state.y1());
        try {
            var q=new org.joml.Quaternionf().rotationZ((float)Math.PI).rotateX(elevation).rotateY(yaw);
            LegacyModels.preview(g,state.player(),cx,cy,state.scale(),GuiCapture.array(GuiCapture.array(0,-.1f,0),GuiCapture.array(q.x,q.y,q.z,q.w),com.google.gson.JsonNull.INSTANCE));
            if(state.transparency()>=100)return;
            float unit=1.6f/176*state.size()/100f;
            double pitch=Math.toRadians(state.pitch());
            var shape=PanelShape.mesh(WIDTH*unit/2,HEIGHT*unit/2,state.curvature(),state.mirror(),false,state.reverse());
            double lowest=Double.POSITIVE_INFINITY;
            for(var vertex:shape)lowest=Math.min(lowest,vertex.y()*Math.cos(pitch)-vertex.z()*Math.sin(pitch));
            double height=Math.max(1.55+state.offset()/100.0,.025-lowest);
            boolean back=PanelShape.backView(Math.sin(yaw)*Math.cos(elevation),-Math.sin(elevation),-Math.cos(yaw)*Math.cos(elevation),0,state.pitch());
            shape=PanelShape.mesh(WIDTH*unit/2,HEIGHT*unit/2,state.curvature(),state.mirror(),back,state.reverse());
            int color=PanelShape.legacyVertexColor(Math.round(255*(1-state.transparency()/100f)),state.brightness());
            var pose=g.pose();pose.pushPose();
            try {
                pose.translate(cx,cy,50);pose.scale(state.scale(),state.scale(),-state.scale());
                pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180));pose.mulPose(com.mojang.math.Axis.XP.rotation(elevation));pose.mulPose(com.mojang.math.Axis.YP.rotation(yaw));
                pose.translate(0,-.1f,0);pose.translate(0,height,dev.wigo.config.SharingRules.PANEL_FORWARD_OFFSET);
                pose.mulPose(com.mojang.math.Axis.XP.rotation((float)pitch));
                var buffers=mc.renderBuffers().bufferSource();var vertices=buffers.getBuffer(type);
                for(var vertex:shape)vertices.addVertex(pose.last(),vertex.x(),vertex.y(),vertex.z()).setUv(vertex.u(),vertex.v()).setColor(color);
                buffers.endBatch(type);
            } finally {pose.popPose();}
        } finally {g.disableScissor();com.mojang.blaze3d.platform.Lighting.setupFor3DItems();}
    }
    public static void closePreview() {
        var mc=Minecraft.getInstance();if(target!=null){mc.getTextureManager().release(id);target=null;}
        sample=null;mannequin=null;rendered=-1;
    }

    private static final class PreviewTexture extends AbstractTexture {
        private final TextureTarget target;
        PreviewTexture(TextureTarget target) {
            this.target = target; id = target.getColorTextureId();
            setFilter(false,false);
        }
        @Override public void load(net.minecraft.server.packs.resources.ResourceManager resources) {}
        @Override public void close() { GuiScratch.destroy(target); id = -1;  }
    }
}
