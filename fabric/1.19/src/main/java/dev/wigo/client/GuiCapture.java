package dev.wigo.client;

import com.google.gson.*;
import dev.wigo.*;
import dev.wigo.mixin.*;
import dev.wigo.protocol.*;
import dev.wigo.platform.Platform;
import net.minecraft.client.Minecraft;
import dev.wigo.client.GuiGraphics;
import dev.wigo.client.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.*;
import dev.wigo.client.LegacyMapState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Matrix3x2f;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.joml.Matrix3x2fc;
import java.util.*;

/** Records semantic draw calls, not rendered pixels, for known compatible screens. */
public final class GuiCapture {
    private static Recorder recorder;
    private static boolean needed,richText;
    private static net.minecraft.client.gui.components.AbstractWidget currentWidget;
    private static Float centeredAnchor;
    private static long session = -1, revision, lastCapture;
    private static Screen failedScreen;
    private static Frame latest, sent, uploading;
    private static final Deque<JsonObject> outgoing = new ArrayDeque<>();
    public static int muted, imageDepth;
    public static LivingEntity inventoryEntity;
    public static float inventoryMouseX, inventoryMouseY;

    private static boolean vanillaScreen(Screen screen) {
        Class<?> type=screen.getClass();
        if(!type.getName().startsWith("net.minecraft.") || type.getClassLoader()!=Screen.class.getClassLoader())return false;
        var actual=type.getProtectionDomain().getCodeSource();
        var vanilla=Screen.class.getProtectionDomain().getCodeSource();
        // Installed legacy Forge supplies no CodeSource for Minecraft classes.
        if(actual==null && vanilla==null)return true;
        return actual!=null && vanilla!=null && actual.getLocation().equals(vanilla.getLocation());
    }
    public static String classify(Screen screen) {
        if (screen == null || !Settings.enabled || screen instanceof ObserverPositionScreen) return "none";
        if (screen instanceof ChatScreen chat && !chatAllowed(chat)) return "none";
        if (WigoLinks.owns(screen)) return Settings.menuEnabled(dev.wigo.config.MenuCatalog.SETTINGS)
                && WigoClient.serverAllows(dev.wigo.config.MenuCatalog.SETTINGS) ? "gui" : "none";
        if (screen == failedScreen || (!VanillaGui.knownScreen(screen)
                && !optifineScreen(screen))) return "unsupported";
        if (screen instanceof AbstractContainerScreen<?> container
                && (!container.getMenu().getClass().getName().startsWith("net.minecraft.")
                ||dev.wigo.config.MenuTypes.id(container.getMenu()).equals(dev.wigo.config.MenuCatalog.OTHER))) return "unsupported";
        if (screen instanceof ContainerScreen && ((AbstractContainerScreen<?>)screen).getMenu() instanceof net.minecraft.world.inventory.ChestMenu chest)
            return chest.getRowCount()>=1&&chest.getRowCount()<=6&&chest.slots.size()==chest.getRowCount()*9+36?"chest":"unsupported";
        if(screen instanceof InventoryScreen&&((InventoryScreen)screen).getMenu().slots.size()!=46)return "unsupported";
        // Inventory with its recipe book/effects uses the complete vanilla description too.
        if (screen instanceof InventoryScreen && !((RecipeBookAccessor)screen).wigoRecipeBook().isVisible()
                && Minecraft.getInstance().player.getActiveEffects().isEmpty()) return "inventory";
        return "gui";
    }
    public static boolean chatAllowed(ChatScreen screen) {
        var input=((ChatAccessor)screen).wigoInput();
        return Settings.menuEnabled(dev.wigo.config.MenuCatalog.CHAT)
                &&WigoClient.serverAllows(dev.wigo.config.MenuCatalog.CHAT)
                &&(Settings.shareCommands||input==null||!input.getValue().startsWith("/"));
    }
    public static boolean decoration(Screen screen,Object widget) {
        if(widget instanceof SettingsPreviewWidget)return true;
        if(WigoLinks.owns(screen))return false;
        return loaderDecoration(widget)||pauseDecoration(screen,widget)
                ||!optifineWidget(widget)&&VanillaGui.addedWidget(widget);
    }
    public static boolean decoration(Object widget) {
        return recorder!=null&&decoration(recorder.screen,widget);
    }
    private static boolean optifineScreen(Screen screen) {
        if (screen.getClass().getClassLoader() != Screen.class.getClassLoader()) return false;
        return switch (screen.getClass().getName()) {
            // Minecraft installs this replacement when the player opens normal chat.
            case "net.optifine.gui.GuiChatOF" -> screen instanceof ChatScreen;
            case "net.optifine.gui.GuiAnimationSettingsOF", "net.optifine.gui.GuiDetailSettingsOF",
                    "net.optifine.gui.GuiOtherSettingsOF", "net.optifine.gui.GuiPerformanceSettingsOF",
                    "net.optifine.gui.GuiQualitySettingsOF", "net.optifine.gui.GuiScreenCapeOF",
                    "net.optifine.gui.GuiMessage", "net.optifine.shaders.gui.GuiShaders",
                    "net.optifine.shaders.gui.GuiShaderOptions" -> true;
            default -> false;
        };
    }
    private static boolean optifineWidget(Object widget) {
        if (widget.getClass().getClassLoader() != Screen.class.getClassLoader()) return false;
        return switch (widget.getClass().getName()) {
            case "net.optifine.gui.GuiButtonOF", "net.optifine.gui.GuiScreenButtonOF",
                    "net.optifine.shaders.gui.GuiButtonDownloadShaders",
                    "net.optifine.shaders.gui.GuiButtonEnumShaderOption",
                    "net.optifine.shaders.gui.GuiButtonShaderOption",
                    "net.optifine.shaders.gui.GuiSliderShaderOption" ->
                    widget instanceof net.minecraft.client.gui.components.AbstractWidget;
            case "net.optifine.shaders.gui.GuiSlotShaders" -> true;
            default -> false;
        };
    }
    public static String panelName(Screen screen) {
        if(screen==null)return "";
        String name=screen.getTitle().getString().strip();
        if(name.isEmpty())name=screen.getClass().getSimpleName();
        if(name.isEmpty())name=screen.getClass().getName();
        return name.length()<=256?name:name.substring(0,256);
    }
    public static boolean richText(){return richText&&WigoClient.richText();}
    public static net.minecraft.client.gui.components.AbstractWidget widget(net.minecraft.client.gui.components.AbstractWidget widget){var previous=currentWidget;currentWidget=widget;return previous;}
    public static Float centered(Float anchor){var previous=centeredAnchor;centeredAnchor=anchor;return previous;}
    public static void demand(long wantedSession,boolean value){demand(wantedSession,value,WigoClient.richText());}
    public static void demand(long wantedSession,boolean value,boolean rich) {
        if (wantedSession != WigoClient.currentSession()) return;
        boolean changed=richText!=rich;richText=rich;
        var connection=Minecraft.getInstance().getConnection();if(connection!=null){var peer=NetworkSessions.peer(connection.getConnection());if(peer!=null)peer.richSource=rich;}
        if(changed)resync();
        needed = value;
        if (!needed) { outgoing.clear(); latest = sent = uploading = null; revision = 0; }
    }
    public static boolean wanted() {return needed;}
    public static void session(long next) {
        if (session == next) return;
        session = next; needed = false; revision = 0; outgoing.clear(); latest = sent = uploading = null; recorder = null;
    }
    public static void resync() { revision = 0; sent = uploading = null; outgoing.clear(); }
    public static void reset() { session = -1; needed = false; failedScreen = null; revision = lastCapture = 0; latest = sent = uploading = null; outgoing.clear(); recorder = null;currentWidget=null;centeredAnchor=null; muted = imageDepth = 0; }
    public static void begin(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        if (!Settings.sharing() || !needed || !classify(screen).equals("gui") || System.nanoTime() - lastCapture < 45_000_000L) return;
        String id=MenuIds.of(screen,"gui");
        if(!Settings.menuEnabled(id)||!WigoClient.serverAllows(id))return;
        recorder = new Recorder(screen, graphics); muted = imageDepth = 0; lastCapture = System.nanoTime();
    }
    public static void end() {
        if (recorder == null) return;
        Recorder current = recorder; recorder = null;currentWidget=null;centeredAnchor=null; muted = imageDepth = 0;
        try { latest = current.finish(); }
        catch (RuntimeException invalid) { failedScreen = current.screen; latest = null; outgoing.clear(); Wigo.LOG.debug("Vanilla GUI unsupported: {}", invalid.toString()); }
    }
    public static boolean active() { return recorder != null && muted == 0; }
    public static boolean capturing() {return recorder!=null;}
    public static void layout(GuiLayout view) { if (recorder != null) recorder.view = view; }
    public static ScreenRectangle tooltipCanvas() {
        return active() && recorder.view != null && !(recorder.screen instanceof ChatScreen) ? recorder.canvas() : null;
    }
    public static Overlay overlay() {return new Overlay(recorder);}
    public static final class Overlay implements AutoCloseable {
        private final Recorder owner;
        private Overlay(Recorder owner) {this.owner=owner;if(owner!=null&&owner.overlays++==0)layer();}
        @Override public void close() {if(owner!=null)owner.overlays--;}
    }
    public static void record(Runnable operation) {
        if (!active()) return;
        try {operation.run();} catch (RuntimeException invalid) {fail();}
    }
    public static boolean fabricPageButton(Object widget) {
        return widget.getClass().getName().equals("net.fabricmc.fabric.impl.item.group.FabricCreativeGuiComponents$ItemGroupButtonWidget");
    }
    public static boolean loaderDecoration(Object widget) {
        String name=widget.getClass().getName();
        return fabricPageButton(widget) || name.equals("net.neoforged.neoforge.client.gui.widget.ModsButton")
                || name.equals("net.minecraftforge.client.gui.widget.ModsButton");
    }
    /** Identify vanilla pause controls by translation key, never by the player's language. */
    public static boolean pauseWidget(net.minecraft.client.gui.components.AbstractWidget widget) {
        // 1.19.3 renders its vanilla buttons through a widget container. Muting
        // that parent would also mute every otherwise allowed button inside it.
        if (widget.getClass().getName().startsWith("net.minecraft.")
                && widget instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container
                && container.children().stream().anyMatch(child -> child instanceof net.minecraft.client.gui.components.AbstractWidget nested
                    && pauseWidget(nested))) return true;
        var component = widget.getMessage();
        if (!(component.getContents() instanceof TranslatableContents translated)) return false;
        return switch (translated.getKey()) {
            case "menu.returnToGame", "gui.advancements", "gui.stats", "menu.sendFeedback", "menu.feedback",
                    "menu.reportBugs", "menu.options", "options.worldOptions.button", "menu.multiplayerOptions.button", "menu.shareToLan",
                    "menu.returnToMenu", "menu.disconnect", "menu.savingLevel", "menu.playerReporting",
                    "gui.socialInteractions", "gui.friends.open", "menu.game", "menu.paused" -> true;
            default -> false;
        };
    }
    public static boolean pauseDecoration(Screen screen, Object child) {
        return screen instanceof net.minecraft.client.gui.screens.PauseScreen
                && child instanceof net.minecraft.client.gui.components.AbstractWidget widget && !pauseWidget(widget);
    }
    public static boolean pauseDecoration(Object widget) {
        return recorder != null && pauseDecoration(recorder.screen, widget);
    }
    /** Capture native pause controls without running another mod's screen overlays twice. */
    public static boolean isolatedPause(Screen screen) {
        return screen instanceof net.minecraft.client.gui.screens.PauseScreen&&Platform.get().isModLoaded("essential")
                &&screen.children().stream().anyMatch(child->child instanceof net.minecraft.client.gui.components.AbstractWidget widget&&pauseWidget(widget));
    }
    public static void pauseContents(Screen screen,GuiGraphics graphics,int x,int y,float delta) {
        boolean title=false;
        for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget
                &&widget.visible&&pauseWidget(widget)) {
            widget.render(graphics.pose(),x,y,delta);
            title|=widget.getMessage().equals(screen.getTitle());
        }
        if(!title)graphics.drawCenteredString(Minecraft.getInstance().font,screen.getTitle(),screen.width/2,40,0xFFFFFFFF);
    }
    public static void fail() { if (recorder != null) recorder.failed = true; }
    public static void tick() {
        if(Minecraft.getInstance().screen instanceof ChatScreen chat&&!chatAllowed(chat)) {
            latest=sent=uploading=null;outgoing.clear();return;
        }
        if (!needed || latest == null || !Settings.sharing() || !Platform.get().canSendGui()) return;
        if (outgoing.isEmpty() && !latest.same(sent)) {
            uploading = latest;
            outgoing.addAll(GuiScene.messages(session, ++revision, new JsonPrimitive(uploading.title), uploading.rect, uploading.commands,
                    sent == null ? new JsonArray[0] : sent.commands, uploading.carried, sent == null,WigoClient.compactMode(),WigoClient.packetTemplates()));
            for(var packet:outgoing){
                if(uploading.literalTitle&&packet.has("title"))packet.addProperty("literalTitle",true);
                if(packet.has("changes")){
                    var literals=new JsonArray();for(int index:uploading.literals)if(packet.getAsJsonObject("changes").has(Integer.toString(index)))literals.add(index);
                    if(!literals.isEmpty())packet.add("literalChanges",literals);
                }
            }
        }
        for (int i = 0; i < 4 && !outgoing.isEmpty(); i++) NetworkBatch.send(WigoClient.guiPayload(outgoing.removeFirst().toString()));
        if (outgoing.isEmpty() && uploading != null) { sent = uploading; uploading = null; }
    }
    public static float[] pointer(float x, float y) {
        if (latest == null) return new float[]{-16, -16};
        float[] point = latest.pointer.point(x, y);
        return new float[]{(point[0] - latest.rect.get(0).getAsFloat()) * latest.scale(),
                (point[1] - latest.rect.get(1).getAsFloat()) * latest.scale()};
    }

    public static JsonArray command(String kind, Matrix3x2fc pose, ScreenRectangle clip, Object... values) {
        JsonArray command = new JsonArray(); command.add(kind);
        if (pose == null || pose.m00() == 1 && pose.m11() == 1 && pose.m01() == 0 && pose.m10() == 0 && pose.m20() == 0 && pose.m21() == 0) command.add(JsonNull.INSTANCE);
        else command.add(array(pose.m00(), pose.m01(), pose.m10(), pose.m11(), pose.m20(), pose.m21()));
        command.add(clip == null ? JsonNull.INSTANCE : array(clip.left(), clip.top(), clip.right(), clip.bottom()));
        for (Object value : values) command.add(json(value));
        return command;
    }
    public static JsonArray array(Object... values) { JsonArray a = new JsonArray(); for (Object value : values) a.add(json(value)); return a; }
    private static JsonElement json(Object v) {
        if (v == null) return JsonNull.INSTANCE;
        if (v instanceof JsonElement j) return j;
        if (v instanceof String s) return new JsonPrimitive(s);
        if (v instanceof Boolean b) return new JsonPrimitive(b);
        if (v instanceof Number n) return new JsonPrimitive(n instanceof Float || n instanceof Double ? Math.round(n.doubleValue() * 65536) / 65536.0 : n);
        throw new IllegalArgumentException("Drawing value");
    }
    public static boolean recording() { return recorder!=null; }
    public static Matrix3x2f pose(dev.wigo.client.GuiGraphics graphics) {
        return pose(graphics.pose().last().pose());
    }
    public static Matrix3x2f pose(com.mojang.math.Matrix4f m) {
        m=LegacyCapturePose.combine(m);var j=LegacyMatrices.joml(m);return new Matrix3x2f(j.m00(),j.m01(),j.m10(),j.m11(),j.m30(),j.m31());
    }
    public static void rectangle(dev.wigo.client.GuiGraphics g,int x0,int y0,int x1,int y1,int top,int bottom) {
        if(!active())return;
        var matrix=pose(g);var bounds=bounds(matrix,x0,y0,x1,y1);
        if(bounds.width()>=recorder.screen.width-4 && (bounds.height()>=recorder.screen.height-4 || bounds.height()<=4 && (top&0xFFFFFF)==0 && (bottom&0xFFFFFF)==0))return;
        add(command("r",matrix,recorder.clip(),x0,y0,x1,y1,top,bottom),bounds);
    }
    public static void text(com.mojang.math.Matrix4f m,net.minecraft.util.FormattedCharSequence value,float x,float y,int color,boolean shadow,int background) {
        if(!active())return;
        StringBuilder plain=new StringBuilder();
        value.accept((index,style,point)->{plain.appendCodePoint(point);return plain.length()<=2048;});
        if(plain.length()==0)return;var matrix=pose(m);int left=Math.round(x),top=Math.round(y);
        JsonArray command=command("t",matrix,recorder.clip(),plain.toString(),left,top,color,shadow,background);
        GuiTextStyles.add(command,value);
        if(recorder.screen instanceof WigoScreen&&recorder.overlays==0)
            GuiTextStyles.wigoContrast(command,currentWidget instanceof net.minecraft.client.gui.components.AbstractButton
                    ||currentWidget instanceof net.minecraft.client.gui.components.AbstractSliderButton
                    ||currentWidget instanceof net.minecraft.client.gui.components.EditBox);
        boolean literal=currentWidget instanceof net.minecraft.client.gui.components.EditBox||TextProvenance.literal(value);
        if(richText()&&!literal) {
            var ref=TextProvenance.reference(value);
            if(ref!=null&&ref.toString().length()<=2048){command.set(3,ref);if(command.size()==10&&command.get(9).getAsJsonArray().size()==1)command.get(9).getAsJsonArray().get(0).getAsJsonArray().set(0,ref.deepCopy());}
            int measured=Minecraft.getInstance().font.width(value);Float anchor=centeredAnchor;int available=0;
            if(currentWidget instanceof net.minecraft.client.gui.components.AbstractButton||currentWidget instanceof net.minecraft.client.gui.components.AbstractSliderButton) {
                var w=currentWidget;
                if(Math.abs(x-(w.x+w.getWidth()/2-measured/2))<=2&&measured<=w.getWidth()-6){anchor=(float)(w.x+w.getWidth()/2);available=w.getWidth()-8;}
            }
            if(anchor!=null){if(command.size()==9)command.add(JsonNull.INSTANCE);command.add(array(0,anchor,Math.max(1,available)));}
        }
        int index=recorder.commands.size();add(command,bounds(matrix,left,top,left+Minecraft.getInstance().font.width(value)+(shadow?1:0),top+10));
        if(literal&&recorder.commands.size()>index)recorder.literals.add(index);
    }
    public static void model(JsonArray command,ScreenRectangle bounds) { if(active())add(command,bounds); }
    public static ScreenRectangle clip() { return recorder==null?null:recorder.clip(); }
    public static ScreenRectangle tooltipLocal(dev.wigo.client.GuiGraphics graphics) {
        var canvas=tooltipCanvas();if(canvas==null)return null;
        var inverse=new org.joml.Matrix3x2f(pose(graphics)).invert();
        return bounds(inverse,canvas.left(),canvas.top(),canvas.right(),canvas.bottom());
    }
    public static void caret(dev.wigo.client.GuiGraphics graphics,int x,int y,int color,boolean insert,boolean shadow,long epoch) {
        if(!active())return;
        var matrix=pose(graphics);
        add(command("c",matrix,recorder.clip(),x,y,color,insert,shadow,epoch),bounds(matrix,x,y,x+(insert?1:6),y+10));
    }
    public static void item(GuiGraphics g, ItemStack stack, int x, int y, int seed) {
        if (!active() || stack.isEmpty()) return;
        add(command("i", pose(g), recorder.clip(), StackJson.encode(stack, Minecraft.getInstance().level.registryAccess()), x, y, seed), bounds(pose(g), x, y, x + 16, y + 16));
    }
    public static void texture(GuiGraphics g, ResourceLocation texture, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int color) {
        if (!active() || imageDepth != 0) return;
        if (!texture.getNamespace().equals("minecraft") && !GuiScene.isWigoIcon(texture.toString())) { fail(); return; }
        if (Set.of("textures/gui/options_background.png", "textures/gui/menu_background.png", "textures/gui/inworld_menu_background.png",
                "textures/gui/menu_list_background.png", "textures/gui/inworld_menu_list_background.png",
                "textures/gui/header_separator.png", "textures/gui/inworld_header_separator.png",
                "textures/gui/footer_separator.png", "textures/gui/inworld_footer_separator.png").contains(texture.getPath())) return;
        for (TextureAtlasSprite sprite : recorder.sprites) {
            if (sprite.atlas().location().equals(texture) && u0 >= sprite.getU0()-0.0001 && u1 <= sprite.getU1()+0.0001 && v0 >= sprite.getV0()-0.0001 && v1 <= sprite.getV1()+0.0001) {
                add(command("a", pose(g), recorder.clip(), texture.toString(), sprite.getName().toString(), x0, y0, x1, y1,
                        (u0-sprite.getU0())/(sprite.getU1()-sprite.getU0()), (u1-sprite.getU0())/(sprite.getU1()-sprite.getU0()),
                        (v0-sprite.getV0())/(sprite.getV1()-sprite.getV0()), (v1-sprite.getV0())/(sprite.getV1()-sprite.getV0()), color), bounds(pose(g), x0, y0, x1, y1)); return;
            }
        }
        add(command("b", pose(g), recorder.clip(), texture.toString(), x0, y0, x1, y1, u0, u1, v0, v1, color), bounds(pose(g), x0, y0, x1, y1));
    }
    public static void sprite(GuiGraphics g, ResourceLocation id, int sourceWidth, int sourceHeight, int u, int v, int x, int y, int width, int height, int color) {
        if (!active() || imageDepth != 0) return;
        if (!id.getNamespace().equals("minecraft")) { fail(); return; }
        add(command("s", pose(g), recorder.clip(), id.toString(), sourceWidth, sourceHeight, u, v, x, y, width, height, color), bounds(pose(g), x, y, x+width, y+height));
    }
    public static void resolvedSprite(TextureAtlasSprite sprite) { if (active() && recorder.sprites.size() < 128) recorder.sprites.add(sprite); }
    public static void atlas(GuiGraphics g,TextureAtlasSprite sprite,int sw,int sh,int u,int v,int x,int y,int w,int h,int color) {
        if (!active()||imageDepth!=0) return;
        if (!sprite.atlas().location().getNamespace().equals("minecraft")||!sprite.getName().getNamespace().equals("minecraft")) {fail();return;}
        add(command("a",pose(g),recorder.clip(),sprite.atlas().location().toString(),sprite.getName().toString(),x,y,x+w,y+h,
                sw==0?0:u/(float)sw,sw==0?1:(u+w)/(float)sw,sh==0?0:v/(float)sh,sh==0?1:(v+h)/(float)sh,color),bounds(pose(g),x,y,x+w,y+h));
    }
    public static void mapPrepared(LegacyMapState state,MapItemSavedData data) {if (active()) recorder.maps.put(state,data);}
    public static void map(org.joml.Matrix3x2f matrix,LegacyMapState state) {
        if (!active()) return;
        MapItemSavedData data=recorder.maps.get(state);
        if (data==null) {fail();return;}
        for (int tile=0;tile<4;tile++) add(command("m",matrix,recorder.clip(),tile*32,MapPalette.encode(data.colors,tile*4096)),bounds(matrix,0,tile*32,128,(tile+1)*32));
        for (var decoration:state.decorations) if (decoration.renderOnFrame) {
            Matrix3x2f pose=new Matrix3x2f(matrix).translate(decoration.x/2f+64,decoration.y/2f+64).rotate((float)Math.PI/180*decoration.rot*360/16).scale(4).translate(-0.125f,0.125f);
            int icon=decoration.icon&255;
            float u0=(icon%16)/16f,u1=(icon%16+1)/16f,v0=(icon/16)/16f,v1=(icon/16+1)/16f;
            add(command("b",pose,recorder.clip(),"minecraft:textures/map/map_icons.png",-1,-1,1,1,u0,u1,v1,v0,-1),bounds(pose,-1,-1,1,1));
            if (decoration.name!=null) {
                String name=decoration.name.getString();int width=Minecraft.getInstance().font.width(name);float scale=dev.wigo.protocol.LegacyClamp.clamp(25f/Math.max(1,width),0,6f/9);
                pose.set(matrix).translate(decoration.x/2f+64-width*scale/2,decoration.y/2f+68).scale(scale);
                add(command("t",pose,recorder.clip(),name,0,0,-1,false,Integer.MIN_VALUE),bounds(pose,0,0,width,9));
            }
        }
    }
    public static void scissor(GuiGraphics g, int x0, int y0, int x1, int y1) {
        if (recorder == null) return;
        ScreenRectangle r = bounds(pose(g), x0, y0, x1, y1);
        ScreenRectangle previous = recorder.clip();
        recorder.clips.push(previous == null ? r : Objects.requireNonNullElse(previous.intersection(r), new ScreenRectangle(0,0,0,0)));
    }
    public static void unscissor() { if (recorder != null && !recorder.clips.isEmpty()) recorder.clips.pop(); }
    public static void layer() { if (active()) add(command("z", null, null, 0), null); }
    public static void chat(String draft) {
        if (!active()) return;
        int count=draft.codePointCount(0,draft.length());
        String preview=draft.substring(0,draft.offsetByCodePoints(0,Math.min(10,count)));
        add(command("r",null,null,0,0,96,18,0x50000000,0x50000000),new ScreenRectangle(0,0,96,18));
        int index=recorder.commands.size();add(command("t",null,null,preview,5,5,0xFFFFFFFF,false,0),new ScreenRectangle(0,0,96,18));if(recorder.commands.size()>index)recorder.literals.add(index);
        add(command("q",null,null,draft),null);
    }
    private static void add(JsonArray command, ScreenRectangle bounds) {
        if (recorder == null || recorder.failed) return;
        if(bounds!=null&&currentWidget==null&&recorder.overlays==0&&!WigoLinks.owns(recorder.screen)
                &&!(recorder.screen instanceof ChatScreen)&&!VanillaGui.nativeDrawing(recorder.graphics.getClass()))return;
        if (bounds != null) {
            ScreenRectangle clip = recorder.clip();
            if (clip != null) bounds = bounds.intersection(clip);
            if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) return;
            if(recorder.overlays==0) {
                recorder.left = Math.min(recorder.left, bounds.left()); recorder.top = Math.min(recorder.top, bounds.top());
                recorder.right = Math.max(recorder.right, bounds.right()); recorder.bottom = Math.max(recorder.bottom, bounds.bottom());
            }
        }
        recorder.commands.add(command);
        recorder.chars += command.toString().length();
        if (recorder.commands.size() > GuiScene.MAX_COMMANDS || recorder.chars > GuiScene.MAX_SCENE_CHARS) recorder.failed = true;
    }
    private static ScreenRectangle bounds(Matrix3x2fc pose,int x0,int y0,int x1,int y1) {
        return LegacyBounds.transform(new ScreenRectangle(Math.min(x0,x1),Math.min(y0,y1),Math.abs(x1-x0),Math.abs(y1-y0)),
                LegacyMatrices.affine(pose.m00(),pose.m01(),pose.m10(),pose.m11(),pose.m20(),pose.m21()));
    }
    private static final class Recorder {
        final Screen screen; final GuiGraphics graphics;
        final Set<Integer> literals=new LinkedHashSet<>();
        final List<JsonArray> commands = new ArrayList<>(); final List<TextureAtlasSprite> sprites = new ArrayList<>();
        final Deque<ScreenRectangle> clips = new ArrayDeque<>();
        final Map<LegacyMapState,MapItemSavedData> maps=new IdentityHashMap<>();
        int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = Integer.MIN_VALUE, bottom = Integer.MIN_VALUE, chars;
        boolean failed;
        int overlays;
        GuiLayout view;
        Recorder(Screen screen, GuiGraphics graphics) { this.screen = screen; this.graphics = graphics; }
        ScreenRectangle clip() { return clips.peek(); }
        ScreenRectangle canvas() {
            if (left == Integer.MAX_VALUE) return new ScreenRectangle(view.left(),view.top(),GuiLayout.WIDTH,GuiLayout.HEIGHT);
            // Include persistent sidebars, such as recipes/effects, before fitting overlays.
            int x=Math.min(view.left(),left),y=Math.min(view.top(),top);
            return new ScreenRectangle(x,y,Math.max(GuiLayout.WIDTH,right-x),Math.max(GuiLayout.HEIGHT,bottom-y));
        }
        Frame finish() {
            if (failed || left == Integer.MAX_VALUE) throw new IllegalArgumentException("Unsupported drawing element");
            for (JsonArray command : commands) GuiScene.validateCommand(command);
            JsonElement carried = screen instanceof AbstractContainerScreen<?> c ? StackJson.encode(c.getMenu().getCarried(), Minecraft.getInstance().level.registryAccess()) : JsonNull.INSTANCE;
            ScreenRectangle canvas=canvas();
            JsonArray rect = screen instanceof ChatScreen ? array(left,top,Math.max(1,right-left),Math.max(1,bottom-top))
                    : array(canvas.left(),canvas.top(),canvas.width(),canvas.height());
            return new Frame(screen.getTitle().getString(), rect, commands.toArray(JsonArray[]::new), carried, view.pointer(),Set.copyOf(literals),!(screen.getTitle().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents));
        }
    }
    private record Frame(String title, JsonArray rect, JsonArray[] commands, JsonElement carried, GuiLayout.PointerMap pointer,Set<Integer> literals,boolean literalTitle) {
        boolean same(Frame other) { return other != null && title.equals(other.title) && rect.equals(other.rect) && Arrays.equals(commands, other.commands) && carried.equals(other.carried)&&literals.equals(other.literals)&&literalTitle==other.literalTitle; }
        float scale() { return Math.min(1, Math.min(512 / rect.get(2).getAsFloat(), 384 / rect.get(3).getAsFloat())); }
    }
}
