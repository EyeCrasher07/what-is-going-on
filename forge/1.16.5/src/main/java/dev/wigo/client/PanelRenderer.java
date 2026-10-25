package dev.wigo.client;

import net.minecraft.client.shader.Framebuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.shader.Framebuffer;
import dev.wigo.mixin.RenderTypeInvoker;
import dev.wigo.Wigo;
import dev.wigo.config.SharingRules;
import net.minecraft.client.Minecraft;
import dev.wigo.client.GuiGraphics;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Matrix4f;
import org.joml.Vector4f;
import java.util.*;

/** GPU-only local rendering. No readback, images, or frame files. */
public final class PanelRenderer {
    private static final int PAD = 16, SCALE = 3, WIDTH = 176;
    private static final ResourceLocation CHEST = new ResourceLocation("minecraft","textures/gui/container/generic_54.png");
    private static final ResourceLocation INVENTORY = new ResourceLocation("minecraft","textures/gui/container/inventory.png");
    private static final ResourceLocation[] ARMOR = {PlayerContainer.EMPTY_ARMOR_SLOT_HELMET, PlayerContainer.EMPTY_ARMOR_SLOT_CHESTPLATE,
            PlayerContainer.EMPTY_ARMOR_SLOT_LEGGINGS, PlayerContainer.EMPTY_ARMOR_SLOT_BOOTS};
    private static final Map<UUID, Surface> surfaces = new HashMap<>();
    private static BufferBuilder classicStorage;
    private static IRenderTypeBuffer.Impl classicBuffer;
    private static boolean depthTestNameTags;
    private static Framebuffer classicWorldDepth;
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
    private static MatrixStack.Entry copyPose(MatrixStack.Entry source) {
        var copy = new MatrixStack().last();
        copy.pose().multiply(source.pose());copy.normal().load(source.normal());return copy;
    }
    @com.github.bsideup.jabel.Desugar
    private record ClassicPanel(MatrixStack.Entry pose,RenderType type,PanelShape.Vertex[] shape,int color,double distance) {}

    private static boolean hidden(RemoteMenu menu) {
        return !Settings.showUnsupportedMenus && (menu.state.kind.equals("unsupported") || menu.renderUnsupported);
    }

    /** Scoped to the render thread's submission of one entity's name and score. */
    public static boolean beginNameTags(Entity state,ActiveRenderInfo camera) {
        boolean previous=depthTestNameTags;depthTestNameTags=false;
        Minecraft mc=Minecraft.getInstance();
        if(Settings.receiving()&&Settings.transparency<100&&mc.level!=null&&mc.player!=null&&state instanceof PlayerEntity actor) {
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
    public static double cameraOpacity(RemoteMenu menu,PlayerEntity actor,ActiveRenderInfo camera) {
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
        var savedState = new LegacyGlState();

        try {
            for (RemoteMenu menu : WigoClient.menus.values()) {
                if (!menu.state.ready || hidden(menu)) continue;
                PlayerEntity actor = mc.level.getPlayerByUUID(menu.owner);
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
            savedState.close();
        }
    }

    static void draw(GuiGraphics g, RemoteMenu menu, PlayerEntity actor, GuiReplay replay) {
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
            g.drawString(mc.font, menu.state.title == null ? new net.minecraft.util.text.TranslationTextComponent("container.chest").getVisualOrderText()
                    : net.minecraft.util.IReorderingProcessor.forward(menu.state.title,net.minecraft.util.text.Style.EMPTY), 8, 6, 0xFF404040, false);
            g.drawString(mc.font,menu.state.inventoryCaption==null?new net.minecraft.util.text.TranslationTextComponent("container.inventory").getVisualOrderText():net.minecraft.util.IReorderingProcessor.forward(menu.state.inventoryCaption,net.minecraft.util.text.Style.EMPTY), 8, height - 94, 0xFF404040, false);
        } else {
            g.blit( INVENTORY, 0, 0, 0, 0, WIDTH, height, 256, 256);
            g.drawString(mc.font,menu.state.craftingCaption==null?new net.minecraft.util.text.TranslationTextComponent("container.crafting").getVisualOrderText():net.minecraft.util.IReorderingProcessor.forward(menu.state.craftingCaption,net.minecraft.util.text.Style.EMPTY), 97, 6, 0xFF404040, false);
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
                    ResourceLocation icon = i >= 5 && i <= 8 ? ARMOR[i - 5] : i == 45 ? PlayerContainer.EMPTY_ARMOR_SLOT_SHIELD : null;
                    if (icon != null) g.blit(xy[0],xy[1],0,16,16,mc.getTextureAtlas(PlayerContainer.BLOCK_ATLAS).apply(icon));
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
        String title=menu.scene!=null&&!dev.wigo.compat.Java8.isBlank(menu.scene.title)?menu.scene.title:menu.state.panel;
        ITextComponent label=dev.wigo.compat.Java8.isBlank(title)?new net.minecraft.util.text.TranslationTextComponent("wigo.unsupported"):
                new net.minecraft.util.text.TranslationTextComponent("wigo.unsupported.named",title);
        var lines=mc.font.split(label,Math.max(16,width-16));
        int y=Math.max(8,(height-lines.size()*mc.font.lineHeight)/2);
        for(var line:lines) {g.drawString(mc.font,line,8,y,0xFFFFFFFF,false);y+=mc.font.lineHeight;}
    }

    public static int[] position(RemoteMenu menu, int slot) {
        return MenuSlots.position(menu.state.kind,menu.state.rows,slot);
    }

    public static void submit(MatrixStack pose,IRenderTypeBuffer.Impl buffers,ActiveRenderInfo camera,float delta) {
        Minecraft mc = Minecraft.getInstance();
        classicPanels.clear();classicTypes.clear();
        if (mc.level == null || !Settings.receiving() || Settings.transparency>=100) return;

        for (RemoteMenu menu : WigoClient.menus.values()) {
            Surface surface = surfaces.get(menu.owner);
            if (surface == null || !menu.state.ready || hidden(menu)) continue;
            PlayerEntity actor = mc.level.getPlayerByUUID(menu.owner);
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
            int ambient=Settings.autoBrightness?ambientLight(mc.level,new net.minecraft.util.math.BlockPos(
                    (int)Math.floor(center.x),(int)Math.floor(center.y),(int)Math.floor(center.z))):15;
            int brightness=surface.lighting.update(Settings.autoBrightness,Settings.brightness,ambient,System.nanoTime());
            pose.pushPose();
            pose.translate(p.x - camera.getPosition().x - Math.sin(radians) * forward,
                    p.y - camera.getPosition().y + panelHeight, p.z - camera.getPosition().z + Math.cos(radians) * forward);
            pose.mulPose(net.minecraft.util.math.vector.Vector3f.YP.rotation((float)-radians));
            pose.mulPose(net.minecraft.util.math.vector.Vector3f.XP.rotation((float)Math.toRadians(Settings.panelPitch)));
            boolean backView=PanelShape.backView(camera.getPosition().x-center.x,camera.getPosition().y-center.y,camera.getPosition().z-center.z,menu.state.yaw,Settings.panelPitch);
            var shape=surface.shape(backView);int alpha=(int)Math.round(255*opacity),color=LegacyPanelType.color(alpha,brightness);
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
                classicBuffer=IRenderTypeBuffer.immediate(classicStorage);
            }
            classicPanels.sort(Comparator.comparingDouble(ClassicPanel::distance).reversed());
            for(var panel:classicPanels) {
                vertices(classicBuffer.getBuffer(panel.type),panel.pose,panel.shape,panel.color);
                classicTypes.add(panel.type);
            }
            classicPanels.clear();
        }
    }

    private static void vertices(IVertexBuilder vertices,MatrixStack.Entry pose,PanelShape.Vertex[] shape,int color) {
        for(var vertex:shape)vertices.vertex(pose.pose(),vertex.x(),vertex.y(),vertex.z()).uv(vertex.u(),vertex.v()).color((color>>16)&255,(color>>8)&255,color&255,color>>>24).endVertex();
    }

    public static int ambientLight(net.minecraft.client.world.ClientWorld level,net.minecraft.util.math.BlockPos pos) {
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
        final Framebuffer target; final RenderType type,translucentType;
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
    private static final class SurfaceTexture extends Texture {
        private final Framebuffer target;
        SurfaceTexture(Framebuffer target) {
            this.target = target; id = target.getColorTextureId();
            setFilter(false,false);
        }
        @Override public void load(net.minecraft.resources.IResourceManager resources) {}
        @Override public void close() { GuiScratch.destroy(target); id = -1;  }
    }
}
