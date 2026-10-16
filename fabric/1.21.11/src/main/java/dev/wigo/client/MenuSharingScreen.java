package dev.wigo.client;

import dev.wigo.config.MenuCatalog;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

/** A searchable, scrolling list; server-prohibited rows have text instead of a toggle. */
public final class MenuSharingScreen extends WigoScreen {
    private EditBox search;
    private Button done;
    private MenuList menuList;
    private List<MenuCatalog.Menu> filtered=List.of();
    private String query="";
    private Set<String> lastBlocked=Set.of();
    public MenuSharingScreen(WigoSettingsScreen previous) {super(previous,"wigo.settings.menus.title");}
    public static Component name(String id) {return Component.translatable("wigo.menu."+id.replace(':','.'));}
    @Override protected int preferredHeight(){return 360;}
    @Override protected void init() {
        search=addRenderableWidget(new EditBox(font,0,0,100,20,Component.translatable("wigo.settings.menus.search")));
        search.setMaxLength(64);search.setHint(Component.translatable("wigo.settings.menus.search"));search.setValue(query);
        menuList=addRenderableWidget(new MenuList());
        search.setResponder(text->{query=text;refreshRows(false);});
        done=button(Component.translatable("gui.done"),this::onClose,null);
        layoutWidgets();refreshRows(true);
    }
    private void refreshRows(boolean preserveScroll) {
        double scroll=preserveScroll?menuList.scrollAmount():0;
        String needle=query.strip().toLowerCase(Locale.ROOT);
        filtered=MenuCatalog.menus().stream().filter(m->name(m.id()).getString().toLowerCase(Locale.ROOT).contains(needle)
                ||m.id().toLowerCase(Locale.ROOT).contains(needle)).toList();
        lastBlocked=blocked();
        menuList.setFocused(null);
        menuList.replaceEntries(filtered.stream().map(MenuRow::new).toList());menuList.setScrollAmount(scroll);
    }
    private Set<String> blocked() {
        Set<String> blocked=new HashSet<>();
        for(var menu:filtered)if(!WigoClient.serverAllows(menu.id()))blocked.add(menu.id());
        return Set.copyOf(blocked);
    }
    @Override public void layoutWidgets() {
        int x=bodyX(),w=bodyWidth();boolean small=bodyHeight()<210;
        search.setRectangle(w,20,x,bodyY()+(small?44:76));
        menuList.updateSizeAndPosition(w,Math.max(26,bodyHeight()-(small?118:150)),x,bodyY()+(small?70:102));
        done.setRectangle(w,20,x,bodyBottom()-26);
    }
    @Override public void tick() {if(!blocked().equals(lastBlocked))refreshRows(true);}
    @Override public void render(GuiGraphics g,int x,int y,float delta) {
        super.render(g,x,y,delta);heading(g,null);
        if(bodyHeight()>=210)paragraph(g,Component.translatable("wigo.settings.menus.help"),34,SECONDARY);
        if(filtered.isEmpty())g.drawCenteredString(font,Component.translatable("wigo.settings.menus.empty"),width/2,bodyY()+120,SECONDARY);
        g.drawCenteredString(font,Component.translatable("wigo.settings.menus.count",filtered.size()),width/2,bodyBottom()-41,SECONDARY);
    }
    private final class MenuList extends ContainerObjectSelectionList<MenuRow> {
        MenuList() {super(MenuSharingScreen.this.minecraft,100,26,102,26);}
        @Override public int getRowWidth() {return getWidth()-12;}
        @Override protected int scrollBarX() {return getRight()-6;}
        @Override protected void renderListBackground(GuiGraphics g) {}
        @Override protected void renderListSeparators(GuiGraphics g) {}
    }
    private final class MenuRow extends ContainerObjectSelectionList.Entry<MenuRow> {
        private final MenuCatalog.Menu menu;
        private final Button toggle;
        MenuRow(MenuCatalog.Menu menu) {
            this.menu=menu;
            toggle=WigoClient.serverAllows(menu.id())?Button.builder(value(),b->{Settings.toggleMenu(menu.id());b.setMessage(value());})
                    .bounds(0,0,72,20).tooltip(Tooltip.create(Component.translatable(menu.id().equals(MenuCatalog.SETTINGS)
                            ?"wigo.settings.f8.description":"wigo.settings.menu.description"))).build():null;
        }
        private Component value() {return Component.translatable(Settings.menuEnabled(menu.id())?"options.on":"options.off");}
        private void layoutToggle() {if(toggle!=null)toggle.setRectangle(72,20,getContentRight()-72,getContentY()+1);}
        @Override public void setX(int x) {super.setX(x);layoutToggle();}
        @Override public void setY(int y) {super.setY(y);layoutToggle();}
        @Override public void setWidth(int width) {super.setWidth(width);layoutToggle();}
        @Override public List<? extends GuiEventListener> children() {return toggle==null?List.of():List.of(toggle);}
        @Override public List<? extends NarratableEntry> narratables() {return toggle==null?List.of():List.of(toggle);}
        @Override public void visitWidgets(Consumer<AbstractWidget> visitor) {if(toggle!=null)visitor.accept(toggle);}
        @Override public void renderContent(GuiGraphics g,int mouseX,int mouseY,boolean hovered,float delta) {
            int x=getContentX(),y=getContentY();g.renderItem(MenuIcons.item(menu.id()),x,y+2);
            int labelX=x+24,available=getContentWidth()-24;
            if(toggle==null) {
                g.drawString(font,font.plainSubstrByWidth(name(menu.id()).getString(),available),labelX,y,TEXT,false);
                g.drawString(font,font.plainSubstrByWidth(Component.translatable("wigo.settings.admin.blocked").getString(),available),labelX,y+11,0xFF9C2525,false);
            } else {
                g.drawString(font,font.plainSubstrByWidth(name(menu.id()).getString(),available-80),labelX,y+6,TEXT,false);
                toggle.render(g,mouseX,mouseY,delta);
            }
        }
    }
}
