package dev.wigo.client;

import dev.wigo.config.MenuCatalog;
import dev.wigo.client.GuiGraphics;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import java.util.*;
import java.util.function.Consumer;

/** A searchable, scrolling list; server-prohibited rows have text instead of a toggle. */
public final class MenuSharingScreen extends WigoScreen {
    private TextFieldWidget search;
    private Button done;
    private MenuList menuList;
    private List<MenuCatalog.Menu> filtered=dev.wigo.compat.Java8.list();
    private String query="";
    private Set<String> lastBlocked=dev.wigo.compat.Java8.set();
    public MenuSharingScreen(WigoSettingsScreen previous) {super(previous,"wigo.settings.menus.title");}
    public static ITextComponent name(String id) {return new net.minecraft.util.text.TranslationTextComponent("wigo.menu."+id.replace(':','.'));}
    @Override protected int preferredHeight(){return 360;}
    @Override protected void init() {
        search=addButton(new TextFieldWidget(font,0,0,100,20,new net.minecraft.util.text.TranslationTextComponent("wigo.settings.menus.search")));
        search.setMaxLength(64);LegacyTooltips.hint(search,new net.minecraft.util.text.TranslationTextComponent("wigo.settings.menus.search"));search.setValue(query);
        menuList=addMenuList(new MenuList());
        search.setResponder(text->{query=text;refreshRows(false);});
        done=button(new net.minecraft.util.text.TranslationTextComponent("gui.done"),this::onClose,null);
        layoutWidgets();refreshRows(true);
    }
    private void refreshRows(boolean preserveScroll) {
        double scroll=preserveScroll?menuList.getScrollAmount():0;
        String needle=dev.wigo.compat.Java8.strip(query).toLowerCase(Locale.ROOT);
        filtered=MenuCatalog.menus().stream().filter(m->name(m.id()).getString().toLowerCase(Locale.ROOT).contains(needle)
                ||m.id().toLowerCase(Locale.ROOT).contains(needle)).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));
        lastBlocked=blocked();
        menuList.setFocused(null);
        menuList.replaceEntries(filtered.stream().map(MenuRow::new).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList)));menuList.setScrollAmount(scroll);LegacyListEntry.layout(menuList);
    }
    private Set<String> blocked() {
        Set<String> blocked=new HashSet<>();
        for(var menu:filtered)if(!WigoClient.serverAllows(menu.id()))blocked.add(menu.id());
        return dev.wigo.compat.Java8.copySet(blocked);
    }
    @Override public void layoutWidgets() {
        int x=bodyX(),w=bodyWidth();boolean small=bodyHeight()<210;
        LegacyWidgetLayout.resize(search,w,20,x,bodyY()+(small?44:76));
        LegacyListEntry.resize(menuList,w,Math.max(26,bodyHeight()-(small?118:150)),x,bodyY()+(small?70:102));
        LegacyWidgetLayout.resize(done,w,20,x,bodyBottom()-26);
    }
    @Override public void tick() {if(!blocked().equals(lastBlocked))refreshRows(true);}
    @Override public void render(com.mojang.blaze3d.matrix.MatrixStack pose,int x,int y,float delta) {var g=GuiGraphics.wrap(pose);
        super.render(g.pose(),x,y,delta);heading(g,null);
        if(bodyHeight()>=210)paragraph(g,new net.minecraft.util.text.TranslationTextComponent("wigo.settings.menus.help"),34,SECONDARY);
        if(filtered.isEmpty())g.drawCenteredString(font,new net.minecraft.util.text.TranslationTextComponent("wigo.settings.menus.empty"),width/2,bodyY()+120,SECONDARY);
        g.drawCenteredString(font,new net.minecraft.util.text.TranslationTextComponent("wigo.settings.menus.count",filtered.size()),width/2,bodyBottom()-41,SECONDARY);
    }
    private final class MenuList extends LegacySelectionList<MenuRow> {
        @Override public void replaceEntries(java.util.Collection<MenuRow> rows) {super.replaceEntries(rows);}
        MenuList() {super(MenuSharingScreen.this.minecraft,100,26,102,26);}
        @Override public int getRowWidth() {return getWidth()-12;}
        @Override protected int getScrollbarPosition() {return getRight()-6;}
    }
    private final class MenuRow extends LegacyListEntry<MenuRow> {
        private final MenuCatalog.Menu menu;
        private final Button toggle;
        MenuRow(MenuCatalog.Menu menu) {
            this.menu=menu;
            toggle=WigoClient.serverAllows(menu.id())?LegacyButtons.builder(value(),b->{Settings.toggleMenu(menu.id());b.setMessage(value());})
                    .bounds(0,0,72,20).tooltip(LegacyTooltips.create(new net.minecraft.util.text.TranslationTextComponent(menu.id().equals(MenuCatalog.SETTINGS)
                            ?"wigo.settings.f8.description":"wigo.settings.menu.description"))).build():null;
        }
        private ITextComponent value() {return new net.minecraft.util.text.TranslationTextComponent(Settings.menuEnabled(menu.id())?"options.on":"options.off");}
        private void layoutToggle() {if(toggle!=null)LegacyWidgetLayout.resize(toggle,72,20,getContentRight()-72,getContentY()+1);}
        @Override public void setX(int x) {super.setX(x);layoutToggle();}
        @Override public void setY(int y) {super.setY(y);layoutToggle();}
        @Override public void setWidth(int width) {super.setWidth(width);layoutToggle();}
        @Override public List<? extends IGuiEventListener> children() {return toggle==null?dev.wigo.compat.Java8.list():dev.wigo.compat.Java8.list(toggle);}
        @Override public void visitWidgets(Consumer<Widget> visitor) {if(toggle!=null)visitor.accept(toggle);}
        @Override public void renderContent(GuiGraphics g,int mouseX,int mouseY,boolean hovered,float delta) {
            int x=getContentX(),y=getContentY();g.renderItem(MenuIcons.item(menu.id()),x,y+2);
            int labelX=x+24,available=getContentWidth()-24;
            if(toggle==null) {
                g.drawString(font,font.plainSubstrByWidth(name(menu.id()).getString(),available),labelX,y,TEXT,false);
                g.drawString(font,font.plainSubstrByWidth(new net.minecraft.util.text.TranslationTextComponent("wigo.settings.admin.blocked").getString(),available),labelX,y+11,0xFF9C2525,false);
            } else {
                g.drawString(font,font.plainSubstrByWidth(name(menu.id()).getString(),available-80),labelX,y+6,TEXT,false);
                toggle.render(g.pose(),mouseX,mouseY,delta);
            }
        }
    }
}
