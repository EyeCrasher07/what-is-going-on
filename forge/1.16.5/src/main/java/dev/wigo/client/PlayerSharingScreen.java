package dev.wigo.client;

import dev.wigo.config.SharingRules;
import dev.wigo.client.GuiGraphics;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import java.util.*;
import java.util.function.Consumer;

/** Online players and saved offline entries share a searchable, scrolling list. */
public final class PlayerSharingScreen extends WigoScreen {
    private Button mode, add, done;
    private TextFieldWidget search;
    private PlayerList playerList;
    private List<SharingRules.PlayerRef> lastOnline=dev.wigo.compat.Java8.list();
    private long lastRevision;
    private int ticks;
    private String query="",error="";
    public PlayerSharingScreen(WigoSettingsScreen previous) {super(previous,"wigo.settings.players.title");}
    @Override protected int preferredHeight(){return 360;}
    @Override protected void init() {
        mode=button(new net.minecraft.util.text.StringTextComponent(""),()->{
            var values=SharingRules.Mode.values();Settings.mode(values[(Settings.rules().mode().ordinal()+1)%values.length]);
            error="";refreshRows(false);
        },"wigo.settings.audience.description");
        search=addButton(new TextFieldWidget(font,0,0,100,20,new net.minecraft.util.text.TranslationTextComponent("wigo.settings.players.search")));
        search.setMaxLength(64);LegacyTooltips.hint(search,new net.minecraft.util.text.TranslationTextComponent("wigo.settings.players.search"));search.setValue(query);
        add=button(new net.minecraft.util.text.TranslationTextComponent("wigo.settings.players.add.name"),this::addByName,"wigo.settings.players.add.name.description");
        playerList=addMenuList(new PlayerList());
        search.setResponder(text->{query=text;error="";refreshRows(false);});
        done=button(new net.minecraft.util.text.TranslationTextComponent("gui.done"),this::onClose,null);
        layoutWidgets();refreshRows(true);
    }
    private List<SharingRules.PlayerRef> onlinePlayers() {
        if(minecraft.getConnection()==null)return dev.wigo.compat.Java8.list();
        return minecraft.getConnection().getOnlinePlayers().stream().map(p->p.getProfile())
                .filter(p->p.getName().matches("[A-Za-z0-9_]{1,16}"))
                .map(p->new SharingRules.PlayerRef(p.getName(),p.getId()))
                .sorted(Comparator.comparing(SharingRules.PlayerRef::name,String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(p->p.uuid().toString())).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));
    }
    private SharingRules.PlayerRef selected(SharingRules.PlayerRef player,List<SharingRules.PlayerRef> saved) {
        return saved.stream().filter(p->p.uuid()!=null?p.uuid().equals(player.uuid()):p.name().equalsIgnoreCase(player.name()))
                .findFirst().orElse(null);
    }
    private void addByName() {
        String name=dev.wigo.compat.Java8.strip(search.getValue());
        if(!name.matches("[A-Za-z0-9_]{1,16}")){error="wigo.settings.players.invalid";return;}
        var player=onlinePlayers().stream().filter(p->p.name().equalsIgnoreCase(name)).findFirst()
                .orElse(new SharingRules.PlayerRef(name,null));
        if(Settings.addPlayer(Settings.rules().mode(),player)){error="";refreshRows(true);}
        else error="wigo.settings.players.duplicate";
    }
    private void refreshRows(boolean preserveScroll) {
        double scroll=preserveScroll?playerList.getScrollAmount():0;
        SharingRules.Mode selectedMode=Settings.rules().mode();
        mode.setMessage(new net.minecraft.util.text.TranslationTextComponent("wigo.settings.audience",new net.minecraft.util.text.TranslationTextComponent(WigoSettingsScreen.modeKey(selectedMode))));
        boolean editing=selectedMode!=SharingRules.Mode.ALL;
        search.setVisible(editing);add.visible=editing;playerList.visible=editing;
        List<SharingRules.PlayerRef> saved=editing?Settings.players(selectedMode):dev.wigo.compat.Java8.list();
        lastOnline=onlinePlayers();lastRevision=Settings.rulesRevision();
        List<PlayerRow> rows=new ArrayList<>();Set<SharingRules.PlayerRef> present=new HashSet<>();
        String needle=dev.wigo.compat.Java8.strip(query).toLowerCase(Locale.ROOT);
        if(editing) {
            for(var player:lastOnline) {
                var entry=selected(player,saved);
                if(entry!=null)present.add(entry);
                if(entry==null&&minecraft.player!=null&&player.uuid().equals(minecraft.player.getUUID()))continue;
                if(matches(player,entry,needle))rows.add(new PlayerRow(player,entry,true,selectedMode));
            }
            for(var player:saved)if(!present.contains(player)&&matches(player,null,needle))
                rows.add(new PlayerRow(player,player,false,selectedMode));
        }
        rows.sort(Comparator.comparing(row->row.player.name(),String.CASE_INSENSITIVE_ORDER));
        playerList.setFocused(null);playerList.replaceEntries(rows);playerList.setScrollAmount(scroll);LegacyListEntry.layout(playerList);
        add.active=editing&&dev.wigo.compat.Java8.strip(query).matches("[A-Za-z0-9_]{1,16}")&&saved.size()<SharingRules.MAX_PLAYERS
                &&saved.stream().noneMatch(p->p.name().equalsIgnoreCase(dev.wigo.compat.Java8.strip(query)));
        layoutWidgets();
    }
    private static boolean matches(SharingRules.PlayerRef player,SharingRules.PlayerRef saved,String needle) {
        return player.name().toLowerCase(Locale.ROOT).contains(needle)
                ||saved!=null&&saved.name().toLowerCase(Locale.ROOT).contains(needle);
    }
    @Override public void tick() {
        if(lastRevision!=Settings.rulesRevision()||++ticks%10==0&&!onlinePlayers().equals(lastOnline))refreshRows(true);
    }
    @Override public void layoutWidgets() {
        int x=bodyX(),w=bodyWidth(),searchY=bodyY()+(bodyHeight()<210?64:88);
        LegacyWidgetLayout.resize(mode,w,20,x,bodyY()+36);LegacyWidgetLayout.resize(search,w-108,20,x,searchY);LegacyWidgetLayout.resize(add,104,20,x+w-104,searchY);
        LegacyListEntry.resize(playerList,w,Math.max(26,bodyBottom()-searchY-74),x,searchY+26);
        LegacyWidgetLayout.resize(done,w,20,x,bodyBottom()-26);
    }
    @Override public void render(com.mojang.blaze3d.matrix.MatrixStack pose,int x,int y,float delta) {var g=GuiGraphics.wrap(pose);
        super.render(g.pose(),x,y,delta);heading(g,null);
        SharingRules.Mode selectedMode=Settings.rules().mode();
        int count=selectedMode==SharingRules.Mode.ALL?0:Settings.players(selectedMode).size();
        if(bodyHeight()>=210||selectedMode==SharingRules.Mode.ALL) {
            String help="wigo.settings.players."+(count==0&&selectedMode!=SharingRules.Mode.ALL?"empty.":"help.")
                    +selectedMode.name().toLowerCase(Locale.ROOT);
            paragraph(g,new net.minecraft.util.text.TranslationTextComponent(error.isEmpty()?help:error),62,error.isEmpty()?SECONDARY:0xFF9C2525);
        }
        if(selectedMode!=SharingRules.Mode.ALL) {
            if(playerList.children().isEmpty())g.drawCenteredString(font,new net.minecraft.util.text.TranslationTextComponent(dev.wigo.compat.Java8.isBlank(query)
                    ?"wigo.settings.players.none":"wigo.settings.players.notfound"),width/2,playerList.getY()+8,SECONDARY);
            g.drawCenteredString(font,new net.minecraft.util.text.TranslationTextComponent("wigo.settings.players.count",count,SharingRules.MAX_PLAYERS),width/2,bodyBottom()-41,SECONDARY);
        }
    }
    private final class PlayerList extends LegacySelectionList<PlayerRow> {
        @Override public void replaceEntries(java.util.Collection<PlayerRow> rows) {super.replaceEntries(rows);}
        PlayerList() {super(PlayerSharingScreen.this.minecraft,100,26,114,26);}
        @Override public int getRowWidth() {return getWidth()-12;}
        @Override protected int getScrollbarPosition() {return getRight()-6;}
    }
    private final class PlayerRow extends LegacyListEntry<PlayerRow> {
        private final SharingRules.PlayerRef player;
        private final boolean online;
        private final Button toggle;
        PlayerRow(SharingRules.PlayerRef player,SharingRules.PlayerRef saved,boolean online,SharingRules.Mode selectedMode) {
            this.player=player;this.online=online;
            toggle=LegacyButtons.builder(new net.minecraft.util.text.TranslationTextComponent(saved==null?"wigo.settings.players.add":"wigo.settings.players.remove.button"),b->{
                if(saved!=null){Settings.removePlayer(selectedMode,saved);error="";}
                else error=Settings.addPlayer(selectedMode,player)?"":"wigo.settings.players.duplicate";
                refreshRows(true);
            }).bounds(0,0,72,20).tooltip(LegacyTooltips.create(new net.minecraft.util.text.TranslationTextComponent(saved==null
                    ?"wigo.settings.players.add.description":"wigo.settings.players.remove"))).build();
            toggle.active=saved!=null||Settings.players(selectedMode).size()<SharingRules.MAX_PLAYERS;
        }
        private void layoutToggle() {LegacyWidgetLayout.resize(toggle,72,20,getContentRight()-72,getContentY()+1);}
        @Override public void setX(int x) {super.setX(x);layoutToggle();}
        @Override public void setY(int y) {super.setY(y);layoutToggle();}
        @Override public void setWidth(int width) {super.setWidth(width);layoutToggle();}
        @Override public List<? extends IGuiEventListener> children() {return dev.wigo.compat.Java8.list(toggle);}
        @Override public void visitWidgets(Consumer<Widget> visitor) {visitor.accept(toggle);}
        @Override public void renderContent(GuiGraphics g,int mouseX,int mouseY,boolean hovered,float delta) {
            int x=getContentX(),y=getContentY(),available=getContentWidth()-80;
            g.drawString(font,font.plainSubstrByWidth(player.name(),available),x,y,TEXT,false);
            g.drawString(font,font.plainSubstrByWidth(new net.minecraft.util.text.TranslationTextComponent(online?"wigo.settings.players.online":"wigo.settings.players.offline").getString(),available),
                    x,y+11,online?0xFF356B35:0xFF666666,false);
            toggle.render(g.pose(),mouseX,mouseY,delta);
        }
    }
}
