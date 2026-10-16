package dev.wigo.client;

import dev.wigo.config.SharingRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

/** Online players and saved offline entries share a searchable, scrolling list. */
public final class PlayerSharingScreen extends WigoScreen {
    private Button mode, add, done;
    private EditBox search;
    private PlayerList playerList;
    private List<SharingRules.PlayerRef> lastOnline=List.of();
    private long lastRevision;
    private int ticks;
    private String query="",error="";
    public PlayerSharingScreen(WigoSettingsScreen previous) {super(previous,"wigo.settings.players.title");}
    @Override protected int preferredHeight(){return 360;}
    @Override protected void init() {
        mode=button(Component.empty(),()->{
            var values=SharingRules.Mode.values();Settings.mode(values[(Settings.rules().mode().ordinal()+1)%values.length]);
            error="";refreshRows(false);
        },"wigo.settings.audience.description");
        search=addRenderableWidget(new EditBox(font,0,0,100,20,Component.translatable("wigo.settings.players.search")));
        search.setMaxLength(64);search.setHint(Component.translatable("wigo.settings.players.search"));search.setValue(query);
        add=button(Component.translatable("wigo.settings.players.add.name"),this::addByName,"wigo.settings.players.add.name.description");
        playerList=addRenderableWidget(new PlayerList());
        search.setResponder(text->{query=text;error="";refreshRows(false);});
        done=button(Component.translatable("gui.done"),this::onClose,null);
        layoutWidgets();refreshRows(true);
    }
    private List<SharingRules.PlayerRef> onlinePlayers() {
        if(minecraft.getConnection()==null)return List.of();
        return minecraft.getConnection().getOnlinePlayers().stream().map(p->p.getProfile())
                .filter(p->p.getName().matches("[A-Za-z0-9_]{1,16}"))
                .map(p->new SharingRules.PlayerRef(p.getName(),p.getId()))
                .sorted(Comparator.comparing(SharingRules.PlayerRef::name,String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(p->p.uuid().toString())).toList();
    }
    private SharingRules.PlayerRef selected(SharingRules.PlayerRef player,List<SharingRules.PlayerRef> saved) {
        return saved.stream().filter(p->p.uuid()!=null?p.uuid().equals(player.uuid()):p.name().equalsIgnoreCase(player.name()))
                .findFirst().orElse(null);
    }
    private void addByName() {
        String name=search.getValue().strip();
        if(!name.matches("[A-Za-z0-9_]{1,16}")){error="wigo.settings.players.invalid";return;}
        var player=onlinePlayers().stream().filter(p->p.name().equalsIgnoreCase(name)).findFirst()
                .orElse(new SharingRules.PlayerRef(name,null));
        if(Settings.addPlayer(Settings.rules().mode(),player)){error="";refreshRows(true);}
        else error="wigo.settings.players.duplicate";
    }
    private void refreshRows(boolean preserveScroll) {
        double scroll=preserveScroll?playerList.getScrollAmount():0;
        SharingRules.Mode selectedMode=Settings.rules().mode();
        mode.setMessage(Component.translatable("wigo.settings.audience",Component.translatable(WigoSettingsScreen.modeKey(selectedMode))));
        boolean editing=selectedMode!=SharingRules.Mode.ALL;
        search.setVisible(editing);add.visible=editing;playerList.visible=editing;
        List<SharingRules.PlayerRef> saved=editing?Settings.players(selectedMode):List.of();
        lastOnline=onlinePlayers();lastRevision=Settings.rulesRevision();
        List<PlayerRow> rows=new ArrayList<>();Set<SharingRules.PlayerRef> present=new HashSet<>();
        String needle=query.strip().toLowerCase(Locale.ROOT);
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
        add.active=editing&&query.strip().matches("[A-Za-z0-9_]{1,16}")&&saved.size()<SharingRules.MAX_PLAYERS
                &&saved.stream().noneMatch(p->p.name().equalsIgnoreCase(query.strip()));
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
        mode.setRectangle(w,20,x,bodyY()+36);search.setRectangle(w-108,20,x,searchY);add.setRectangle(104,20,x+w-104,searchY);
        LegacyListEntry.resize(playerList,w,Math.max(26,bodyBottom()-searchY-74),x,searchY+26);
        done.setRectangle(w,20,x,bodyBottom()-26);
    }
    @Override public void render(GuiGraphics g,int x,int y,float delta) {
        super.render(g,x,y,delta);heading(g,null);
        SharingRules.Mode selectedMode=Settings.rules().mode();
        int count=selectedMode==SharingRules.Mode.ALL?0:Settings.players(selectedMode).size();
        if(bodyHeight()>=210||selectedMode==SharingRules.Mode.ALL) {
            String help="wigo.settings.players."+(count==0&&selectedMode!=SharingRules.Mode.ALL?"empty.":"help.")
                    +selectedMode.name().toLowerCase(Locale.ROOT);
            paragraph(g,Component.translatable(error.isEmpty()?help:error),62,error.isEmpty()?SECONDARY:0xFF9C2525);
        }
        if(selectedMode!=SharingRules.Mode.ALL) {
            if(playerList.children().isEmpty())g.drawCenteredString(font,Component.translatable(query.isBlank()
                    ?"wigo.settings.players.none":"wigo.settings.players.notfound"),width/2,playerList.getY()+8,SECONDARY);
            g.drawCenteredString(font,Component.translatable("wigo.settings.players.count",count,SharingRules.MAX_PLAYERS),width/2,bodyBottom()-41,SECONDARY);
        }
    }
    private final class PlayerList extends ContainerObjectSelectionList<PlayerRow> {
        @Override public void replaceEntries(java.util.Collection<PlayerRow> rows) {super.replaceEntries(rows);}
        PlayerList() {super(PlayerSharingScreen.this.minecraft,100,26,114,26);setRenderBackground(false);}
        @Override public int getRowWidth() {return getWidth()-12;}
        @Override protected int getScrollbarPosition() {return getRight()-6;}
    }
    private final class PlayerRow extends LegacyListEntry<PlayerRow> {
        private final SharingRules.PlayerRef player;
        private final boolean online;
        private final Button toggle;
        PlayerRow(SharingRules.PlayerRef player,SharingRules.PlayerRef saved,boolean online,SharingRules.Mode selectedMode) {
            this.player=player;this.online=online;
            toggle=Button.builder(Component.translatable(saved==null?"wigo.settings.players.add":"wigo.settings.players.remove.button"),b->{
                if(saved!=null){Settings.removePlayer(selectedMode,saved);error="";}
                else error=Settings.addPlayer(selectedMode,player)?"":"wigo.settings.players.duplicate";
                refreshRows(true);
            }).bounds(0,0,72,20).tooltip(Tooltip.create(Component.translatable(saved==null
                    ?"wigo.settings.players.add.description":"wigo.settings.players.remove"))).build();
            toggle.active=saved!=null||Settings.players(selectedMode).size()<SharingRules.MAX_PLAYERS;
        }
        private void layoutToggle() {toggle.setRectangle(72,20,getContentRight()-72,getContentY()+1);}
        @Override public void setX(int x) {super.setX(x);layoutToggle();}
        @Override public void setY(int y) {super.setY(y);layoutToggle();}
        @Override public void setWidth(int width) {super.setWidth(width);layoutToggle();}
        @Override public List<? extends GuiEventListener> children() {return List.of(toggle);}
        @Override public List<? extends NarratableEntry> narratables() {return List.of(toggle);}
        @Override public void visitWidgets(Consumer<AbstractWidget> visitor) {visitor.accept(toggle);}
        @Override public void renderContent(GuiGraphics g,int mouseX,int mouseY,boolean hovered,float delta) {
            int x=getContentX(),y=getContentY(),available=getContentWidth()-80;
            g.drawString(font,font.plainSubstrByWidth(player.name(),available),x,y,TEXT,false);
            g.drawString(font,font.plainSubstrByWidth(Component.translatable(online?"wigo.settings.players.online":"wigo.settings.players.offline").getString(),available),
                    x,y+11,online?0xFF356B35:0xFF666666,false);
            toggle.render(g,mouseX,mouseY,delta);
        }
    }
}
