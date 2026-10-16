package dev.wigo.client;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.pipeline.*;
import dev.wigo.mixin.RenderTypeInvoker;
import dev.wigo.Wigo;
import dev.wigo.config.SharingRules;
import net.minecraft.client.Minecraft;
import dev.wigo.client.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Matrix4f;
import org.joml.Vector4f;
import java.util.*;

/** GPU-only local rendering. No readback, images, or frame files. */
public final class PanelRenderer {
    private static final int PAD = 16, SCALE = 3, WIDTH = 176;
    private static final ResourceLocation CHEST = new ResourceLocation("minecraft","textures/gui/container/generic_54.png");
    private static final ResourceLocation INVENTORY = new ResourceLocation("minecraft","textures/gui/container/inventory.png");
    private static final ResourceLocation[] ARMOR = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};
    private static final Map<UUID, Surface> surfaces = new HashMap<>();
    private static BufferBuilder classicStorage;
    private static MultiBufferSource.BufferSource classicBuffer;
    private static boolean depthTestNameTags;
    private static TextureTarget classicWorldDepth;
    public static boolean preserveWorldDepth() {
        if(surfaces.isEmpty())return false;
        var main=Minecraft.getInstance().getMainRenderTarget();
        if(classicWorldDepth==null||classicWorldDepth.width!=main.width||classicWorldDepth.height!=main.height) {
            if(classicWorldDepth!=null)GuiScratch.destroy(classicWorldDepth);
            classicWorldDepth=GuiScratch.create(main.width,main.height);
        }
        classicWorldDepth.copyDepthFrom(main);main.bindWrite(false);return true;
    }
    public static void restoreWorldDepth() {
        var main=Minecraft.getInstance().getMainRenderTarget();
        main.copyDepthFrom(classicWorldDepth);main.bindWrite(false);
    }
    private static final List<ClassicPanel> classicPanels = new ArrayList<>();
    private static final Set<RenderType> classicTypes = new LinkedHashSet<>();
    private static PoseStack.Pose copyPose(PoseStack.Pose source) {
        var copy = new PoseStack().last();
        copy.pose().load(source.pose());copy.normal().load(source.normal());return copy;
    }
    private record ClassicPanel(PoseStack.Pose pose,RenderType type,PanelShape.Vertex[] shape,int color,double distance) {}

    private static boolean hidden(RemoteMenu menu) {
        return !Settings.showUnsupportedMenus && (menu.state.kind.equals("unsupported") || menu.renderUnsupported);
    }

    /** Scoped to the render thread's submission of one entity's name and score. */
    public static boolean beginNameTags(Entity state,Camera camera) {
        boolean previous=depthTestNameTags;depthTestNameTags=false;
        Minecraft mc=Minecraft.getInstance();
        if(Settings.receiving()&&Settings.transparency<100&&mc.level!=null&&mc.player!=null&&state instanceof Player actor) {
            var menu=WigoClient.menus.get(actor.getUUID());
            depthTestNameTags=menu!=null&&menu.state.ready&&!menu.isChatPreview()&&!hidden(menu)
                    &&surfaces.containsKey(menu.owner)&&actor.distanceToSqr(mc.player)<=Math.pow(menu.state.distance+1,2)
                    &&cameraOpacity(menu,actor,camera)>0;
        }
        return previous;
    }

    public static void endNameTags(boolean previous) {depthTestNameTags=previous;}
    public static boolean nameTagsNeedDepthTest() {return depthTestNameTags;}

    /** Smooth viewers keep data while hidden, and select the visible side with the real camera. */
    public static double cameraOpacity(RemoteMenu menu,Player actor,Camera camera) {
        if(!WigoClient.smoothMenus())return 1;
        var p=actor.getPosition(Minecraft.getInstance().getFrameTime());
        double forward=menu.isChatPreview()&&actor.isSleeping()?0:SharingRules.PANEL_FORWARD_OFFSET;
        return SharingRules.cameraOpacity(menu.state.side,camera.getPosition().x-p.x,camera.getPosition().z-p.z,menu.state.yaw,forward);
    }

    public static void prepare() {
        Minecraft mc = Minecraft.getInstance();
        Iterator<Map.Entry<UUID, Surface>> iterator = surfaces.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            RemoteMenu menu = WigoClient.menus.get(entry.getKey());
            if (menu == null || hidden(menu) || entry.getValue().height != menu.state.height() + PAD * 2 || entry.getValue().width != menu.state.width() + PAD * 2
                    || entry.getValue().session != menu.state.session) {
                entry.getValue().replay.close(); dev.wigo.client.LegacyMenuTextures.release(entry.getValue().id); iterator.remove();
            }
        }
        if (mc.level == null || !Settings.receiving() || Settings.transparency>=100 || WigoClient.menus.isEmpty()) { close(); return; }
        var savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());

        try {
            for (RemoteMenu menu : WigoClient.menus.values()) {
                if (!menu.state.ready || hidden(menu)) continue;
                Player actor = mc.level.getPlayerByUUID(menu.owner);
                if (actor == null) continue;
                Surface surface = surfaces.computeIfAbsent(menu.owner, id -> new Surface(id, menu.state.width() + PAD * 2, menu.state.height() + PAD * 2, menu.state.session,menu.isChatPreview()));
                try (GuiScratch scratch=GuiScratch.open(surface.target,SCALE); GuiViewport viewport = GuiViewport.open(surface.width, surface.height)) {
                    GuiScratch.clear(surface.target);
                    GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
                    draw(graphics, menu, actor, surface.replay);
                    graphics.flush();
                }
            }
        } finally {
            if (savedProjection != null) RenderSystem.setProjectionMatrix(savedProjection);
        }
    }

    private static void draw(GuiGraphics g, RemoteMenu menu, Player actor, GuiReplay replay) {
        Minecraft mc = Minecraft.getInstance();
        int height = menu.state.height();
        g.pose().pushPose(); g.pose().translate(PAD, PAD,0);
        int width = menu.state.width();
        // Keep the native menu silhouette; the world shader discards empty pixels.
        if (menu.state.kind.equals("unsupported") || menu.renderUnsupported) {
            g.fill(0, 0, width, height, 0xFF202020);
            unsupported(g,menu,width,height);
            g.pose().popPose(); return;
        } else if (menu.state.kind.equals("gui")) {
            try {replay.draw(g, menu, actor, PAD);}
            catch (RuntimeException missing) {
                menu.renderUnsupported=true;replay.close();
                Wigo.LOG.warn("Vanilla menu couldn't be recreated locally: {}",missing.toString());
                g.flush();g.fill(0,0,width,height,0xFF202020);
                unsupported(g,menu,width,height);
            }
        } else if (menu.state.kind.equals("chest")) {
            int top = menu.state.rows * 18 + 17;
            g.blit( CHEST, 0, 0, 0, 0, WIDTH, top, 256, 256);
            g.blit( CHEST, 0, top, 0, 126, WIDTH, 96, 256, 256);
            g.drawString(mc.font, menu.state.title == null ? new net.minecraft.network.chat.TranslatableComponent("container.chest").getVisualOrderText()
                    : net.minecraft.util.FormattedCharSequence.forward(menu.state.title,net.minecraft.network.chat.Style.EMPTY), 8, 6, 0xFF404040, false);
            g.drawString(mc.font,menu.state.inventoryCaption==null?new net.minecraft.network.chat.TranslatableComponent("container.inventory").getVisualOrderText():net.minecraft.util.FormattedCharSequence.forward(menu.state.inventoryCaption,net.minecraft.network.chat.Style.EMPTY), 8, height - 94, 0xFF404040, false);
        } else {
            g.blit( INVENTORY, 0, 0, 0, 0, WIDTH, height, 256, 256);
            g.drawString(mc.font,menu.state.craftingCaption==null?new net.minecraft.network.chat.TranslatableComponent("container.crafting").getVisualOrderText():net.minecraft.util.FormattedCharSequence.forward(menu.state.craftingCaption,net.minecraft.network.chat.Style.EMPTY), 97, 6, 0xFF404040, false);
            // Entity PIP bounds are absolute and don't inherit the GUI pose translation.
            LegacyModels.followMouse(g, PAD + 26, PAD + 8, PAD + 75, PAD + 78, 30, 0.0625f,
                    PAD + menu.state.mouseX, PAD + menu.state.mouseY, actor);
        }
        int hover = menu.state.hovered;
        if (hover >= 0 && hover < menu.items.length) {
            int[] xy = position(menu, hover);
            g.fill(xy[0], xy[1], xy[0] + 16, xy[1] + 16, 0x80FFFFFF);
        }
        for (int i = 0; i < menu.items.length; i++) {
            int[] xy = position(menu, i);
            ItemStack stack = menu.displayItem(i);
            if (stack.isEmpty()) {
                if (menu.state.kind.equals("inventory")) {
                    ResourceLocation icon = i >= 5 && i <= 8 ? ARMOR[i - 5] : i == 45 ? InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD : null;
                    if (icon != null) g.blit(xy[0],xy[1],0,16,16,mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(icon));
                }
            } else {
                g.renderItem(actor, stack, xy[0], xy[1], i);
                g.renderItemDecorations(mc.font, stack, xy[0], xy[1]);
            }
        }
        long now = System.nanoTime();
        int x = Math.round(menu.mouseX(now)), y = Math.round(menu.mouseY(now));
        g.flush();
        ItemStack carried=menu.displayCarried();
        if (carried.isEmpty() && hover >= 0 && hover < menu.items.length && !menu.displayItem(hover).isEmpty()) {
            MenuTooltip.draw(g, menu.displayItem(hover), x, y, width, height);
            g.flush();
        }
        if (!carried.isEmpty()) {
            g.renderItem(actor, carried, x - 8, y - 8, -1);
            g.renderItemDecorations(mc.font, carried, x - 8, y - 8);
        }
        if (menu.state.mouseX >= 0 && menu.state.mouseY >= 0 && menu.state.mouseX < width && menu.state.mouseY < height)
            CursorSprites.draw(g,menu.state.cursorStyle,x,y);
        g.pose().popPose();
    }
    private static void unsupported(GuiGraphics g,RemoteMenu menu,int width,int height) {
        var mc=Minecraft.getInstance();
        String title=menu.scene!=null&&!menu.scene.title.isBlank()?menu.scene.title:menu.state.panel;
        Component label=title.isBlank()?new net.minecraft.network.chat.TranslatableComponent("wigo.unsupported"):
                new net.minecraft.network.chat.TranslatableComponent("wigo.unsupported.named",title);
        var lines=mc.font.split(label,Math.max(16,width-16));
        int y=Math.max(8,(height-lines.size()*mc.font.lineHeight)/2);
        for(var line:lines) {g.drawString(mc.font,line,8,y,0xFFFFFFFF,false);y+=mc.font.lineHeight;}
    }

    public static int[] position(RemoteMenu menu, int slot) {
        if (menu.state.kind.equals("chest")) {
            int chestSlots = menu.state.rows * 9;
            if (slot < chestSlots) return new int[]{8 + slot % 9 * 18, 18 + slot / 9 * 18};
            int i = slot - chestSlots;
            return new int[]{8 + i % 9 * 18, menu.state.rows * 18 + (i < 27 ? 31 + i / 9 * 18 : 89)};
        }
        if (slot == 0) return new int[]{154, 28};
        if (slot < 5) return new int[]{98 + (slot - 1) % 2 * 18, 18 + (slot - 1) / 2 * 18};
        if (slot < 9) return new int[]{8, 8 + (slot - 5) * 18};
        if (slot < 36) return new int[]{8 + (slot - 9) % 9 * 18, 84 + (slot - 9) / 9 * 18};
        if (slot < 45) return new int[]{8 + (slot - 36) * 18, 142};
        return new int[]{77, 62};
    }

    public static void submit(PoseStack pose,MultiBufferSource.BufferSource buffers,Camera camera,float delta) {
        Minecraft mc = Minecraft.getInstance();
        classicPanels.clear();classicTypes.clear();
        if (mc.level == null || !Settings.receiving() || Settings.transparency>=100) return;

        for (RemoteMenu menu : WigoClient.menus.values()) {
            Surface surface = surfaces.get(menu.owner);
            if (surface == null || !menu.state.ready || hidden(menu)) continue;
            Player actor = mc.level.getPlayerByUUID(menu.owner);
            if (actor == null || actor.distanceToSqr(mc.player) > Math.pow(menu.state.distance+1,2)) continue;
            double opacity=1-Settings.transparency/100.0;
            if(WigoClient.smoothMenus())opacity*=cameraOpacity(menu,actor,camera)*menu.openingOpacity(System.nanoTime());
            if(opacity<=0)continue;
            var p = actor.getPosition(delta);
            double radians = Math.toRadians(menu.state.yaw);
            boolean sleepingChat=menu.isChatPreview()&&actor.isSleeping();
            double forward=sleepingChat?0:dev.wigo.config.SharingRules.PANEL_FORWARD_OFFSET;
            double panelHeight=(menu.isChatPreview()?(sleepingChat?0.45:1.05):1.55)+Settings.panelOffset/100.0;
            panelHeight=PanelPlacement.height(mc.level,actor,p,p.x-Math.sin(radians)*forward,p.z+Math.cos(radians)*forward,
                    panelHeight,surface.shape(surface.lastBackView),Settings.panelPitch);
            var center=p.add(-Math.sin(radians)*forward,panelHeight,Math.cos(radians)*forward);
            int ambient=Settings.autoBrightness?ambientLight(mc.level,new net.minecraft.core.BlockPos(
                    (int)Math.floor(center.x),(int)Math.floor(center.y),(int)Math.floor(center.z))):15;
            int brightness=surface.lighting.update(Settings.autoBrightness,Settings.brightness,ambient,System.nanoTime());
            pose.pushPose();
            pose.translate(p.x - camera.getPosition().x - Math.sin(radians) * forward,
                    p.y - camera.getPosition().y + panelHeight, p.z - camera.getPosition().z + Math.cos(radians) * forward);
            pose.mulPose(com.mojang.math.Vector3f.YP.rotation((float)-radians));
            pose.mulPose(com.mojang.math.Vector3f.XP.rotation((float)Math.toRadians(Settings.panelPitch)));
            boolean backView=PanelShape.backView(camera.getPosition().x-center.x,camera.getPosition().y-center.y,camera.getPosition().z-center.z,menu.state.yaw,Settings.panelPitch);
            var shape=surface.shape(backView);int alpha=(int)Math.round(255*opacity),color=PanelShape.legacyVertexColor(alpha,brightness);
            RenderType type=alpha<255?surface.translucentType:surface.type;
            if(alpha<255||menu.isChatPreview()) {
                // Vanilla's classic custom geometry runs before water and clouds.
                // Upload now, then blend these panels at the end of that same world pass.
                classicPanels.add(new ClassicPanel(copyPose(pose.last()),type,shape,color,
                        p.add(-Math.sin(radians)*forward,panelHeight,Math.cos(radians)*forward).distanceToSqr(camera.getPosition())));
            } else {vertices(buffers.getBuffer(type),pose.last(),shape,color);buffers.endBatch(type);}
            pose.popPose();
        }
        if(!classicPanels.isEmpty()) {
            if(classicBuffer==null) {
                classicStorage=new BufferBuilder(32768);
                classicBuffer=MultiBufferSource.immediate(classicStorage);
            }
            classicPanels.sort(Comparator.comparingDouble(ClassicPanel::distance).reversed());
            for(var panel:classicPanels) {
                vertices(classicBuffer.getBuffer(panel.type),panel.pose,panel.shape,panel.color);
                classicTypes.add(panel.type);
            }
            classicPanels.clear();
        }
    }

    private static void vertices(VertexConsumer vertices,PoseStack.Pose pose,PanelShape.Vertex[] shape,int color) {
        for(var vertex:shape)vertices.vertex(pose.pose(),vertex.x(),vertex.y(),vertex.z()).uv(vertex.u(),vertex.v()).color(color).endVertex();
    }

    public static int ambientLight(net.minecraft.client.multiplayer.ClientLevel level,net.minecraft.core.BlockPos pos) {
        return level.getMaxLocalRawBrightness(pos,PanelLighting.skyDarken(level.getTimeOfDay(1),level.getRainLevel(1),level.getThunderLevel(1)));
    }

    public static void renderClassicTransparency() {
        NameTagLayers.renderClassic();
        if(classicBuffer!=null)for(var type:classicTypes)classicBuffer.endBatch(type);
        classicTypes.clear();
    }

    public static void finishClassicFrame() {NameTagLayers.endFrame();}

    public static void close() {
        Minecraft mc = Minecraft.getInstance();
        for (Surface surface : surfaces.values()) { surface.replay.close(); dev.wigo.client.LegacyMenuTextures.release(surface.id); }
        surfaces.clear();
        if(classicWorldDepth!=null){GuiScratch.destroy(classicWorldDepth);classicWorldDepth=null;}
        classicPanels.clear();classicTypes.clear();
        NameTagLayers.endFrame();
        if(classicStorage!=null) {classicStorage.clear();classicStorage=null;classicBuffer=null;}
    }

    private static final class Surface {
        final ResourceLocation id; final int height, width; final long session;
        final GuiReplay replay = new GuiReplay();
        final PanelLighting lighting = new PanelLighting();
        final TextureTarget target; final RenderType type,translucentType;
        private PanelShape.Vertex[] shape;
        private int lastSize=-1,lastCurvature=-1;
        private boolean lastMirroring,lastBackView,lastReverseCurvature;
        PanelShape.Vertex[] shape(boolean backView) {
            if(shape==null||lastSize!=Settings.size||lastCurvature!=Settings.curvature||lastMirroring!=Settings.realisticMirroring||lastBackView!=backView||lastReverseCurvature!=Settings.reverseCurvature) {
                lastSize=Settings.size;lastCurvature=Settings.curvature;lastMirroring=Settings.realisticMirroring;
                float unit=1.6f/WIDTH*Settings.size/100f;
                lastBackView=backView;
                lastReverseCurvature=Settings.reverseCurvature;
                shape=PanelShape.mesh(width*unit/2,height*unit/2,Settings.curvature,Settings.realisticMirroring,backView,Settings.reverseCurvature);
            }
            return shape;
        }
        Surface(UUID owner, int width, int height, long session,boolean chat) {
            this.height = height; this.width = width; this.session = session;
            id = new ResourceLocation("wigo", "menu/" + owner);
            target = GuiScratch.create(width * SCALE, height * SCALE);
            Minecraft.getInstance().getTextureManager().register(id, new SurfaceTexture(target));
            type = LegacyPanelType.create(id,chat);
            translucentType=chat?type:LegacyPanelType.create(id,true);
        }
    }
    private static final class SurfaceTexture extends AbstractTexture {
        private final TextureTarget target;
        SurfaceTexture(TextureTarget target) {
            this.target = target; id = target.getColorTextureId();
            setFilter(false,false);
        }
        @Override public void load(net.minecraft.server.packs.resources.ResourceManager resources) {}
        @Override public void close() { GuiScratch.destroy(target); id = -1;  }
    }
}
