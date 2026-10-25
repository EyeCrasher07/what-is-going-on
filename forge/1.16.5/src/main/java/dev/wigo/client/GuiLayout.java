package dev.wigo.client;

import dev.wigo.mixin.*;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.AbstractButton;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraft.client.gui.widget.list.OptionsRowList;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.INestedGuiEventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.EditBookScreen;
import net.minecraft.client.gui.screen.ReadBookScreen;
import net.minecraft.client.gui.screen.GamemodeSelectionScreen;
import java.lang.reflect.Field;
import java.util.*;

/** A bounded presentation pass. Never resizes the window or rebuilds live widgets. */
public final class GuiLayout implements AutoCloseable {
    public static final int WIDTH = 512, HEIGHT = 300;
    private final Screen screen;
    private final int oldWidth, oldHeight;
    private final List<WidgetBox> widgets = new ArrayList<>();
    private final List<WidgetBox> passiveWidgets = new ArrayList<>();
    private final List<ListBox> lists = new ArrayList<>();
    private final Map<Widget,AbstractList<?>> widgetLists = new IdentityHashMap<>();
    private final Set<Widget> scrollChildren = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Field, Integer> offsets = new LinkedHashMap<>();
    private final boolean virtual;
    private boolean changed;
    private boolean nativeScrollLayout;
    private final int left, top;
    private PointerMap pointer;
    private GameModeSwitcherAccessor switcher;
    private int firstMouseX, firstMouseY;
    private boolean firstMouseSet;

    private GuiLayout(Screen screen) {
        this.screen = screen; oldWidth = screen.width; oldHeight = screen.height;
        virtual = !(screen instanceof ContainerScreen<?>) && !(screen instanceof ChatScreen);
        if (screen instanceof ContainerScreen<?> container) {
            var a = (ContainerScreenAccessor)container;
            left = a.wigoLeft() + (a.wigoWidth() - WIDTH) / 2;
            top = a.wigoTop() + (a.wigoHeight() - HEIGHT) / 2;
        } else { left = top = 0; }
        pointer = new PointerMap(0, 0, dev.wigo.compat.Java8.list());
    }
    public static GuiLayout open(Screen screen, int mouseX, int mouseY) {
        GuiLayout view = new GuiLayout(screen);
        try { view.apply(mouseX, mouseY); return view; }
        catch (RuntimeException error) { view.close(); throw error; }
    }
    public int left() { return left; }
    public int top() { return top; }
    public boolean virtual() { return virtual; }
    public PointerMap pointer() { return pointer; }

    private void apply(int mouseX, int mouseY) {
        if (!virtual) return;
        Set<IGuiEventListener> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var child : screen.children()) collect(child, seen);
        if (screen instanceof GamemodeSelectionScreen) {
            switcher=(GameModeSwitcherAccessor)screen;
            for (var slot:switcher.wigoSlots()) collect(slot,seen);
            firstMouseX=switcher.wigoFirstX();firstMouseY=switcher.wigoFirstY();firstMouseSet=switcher.wigoFirstSet();
        }
        discoverLayouts();
        // Added mod buttons must neither enter the transmitted scene nor anchor its layout.
        // Keep their original boxes in widgets so close() restores the owner's whole screen.
        // The settings mannequin is never transmitted and must not anchor pointer mapping.
        var presentationWidgets = widgets.stream().filter(w -> !GuiCapture.decoration(screen,w.widget))
                .filter(w -> !(screen instanceof IngameMenuScreen) || GuiCapture.pauseWidget(w.widget)).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));
        var pauseBounds = screen instanceof IngameMenuScreen
                ? presentationWidgets.stream().filter(w -> w.widget instanceof AbstractButton).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList)) : presentationWidgets;
        var pauseGaps = omittedPauseRows(pauseBounds);
        int dx = WIDTH / 2 - oldWidth / 2, dy = HEIGHT / 2 - oldHeight / 2;
        int pauseLeft=pauseBounds.stream().filter(w->w.visible&&!(false)).mapToInt(w->w.x).min().orElse(0);
        int pauseRight=pauseBounds.stream().filter(w->w.visible&&!(false)).mapToInt(w->w.x+w.width).max().orElse(oldWidth);
        int pauseTop=pauseBounds.stream().filter(w->w.visible&&!(false)).mapToInt(w->w.y).min().orElse(0);
        int pauseBottom=pauseBounds.stream().filter(w->w.visible&&!(false)).mapToInt(w->w.y+w.height).max().orElse(oldHeight);
        // Vanilla's pause grid includes 50px top padding and a 4px cell margin.
        // Match its truncation after subtracting grid height, including short viewports.
        int pauseHeight=pauseBottom-pauseTop+54;
        int removedHeight=pauseGaps.values().stream().mapToInt(Integer::intValue).sum();
        int pauseDy=(HEIGHT-pauseHeight+removedHeight)/4-(oldHeight-pauseHeight)/4;
        changed = true; screen.width = WIDTH; screen.height = HEIGHT;
        if (screen instanceof IngameMenuScreen)
            for (var box : widgets) if (!GuiCapture.pauseWidget(box.widget)) box.widget.visible = false;
        if(screen instanceof BookLayoutCache cache)cache.wigoClearBookLayout();
        if(switcher!=null) {switcher.wigoFirstX(firstMouseX+dx);switcher.wigoFirstY(firstMouseY+dy);}
        for (var entry : offsets.entrySet()) set(entry.getKey(), entry.getValue() + (entry.getKey().getName().equals("leftPos") ? dx : dy));
        if(nativeScrollLayout) ((WigoScreen)screen).layoutWidgets();
        else if(screen instanceof WigoScreen owned)owned.layoutWidgets();
        else for (var box : presentationWidgets) {
            Widget w = box.widget;
            var parent=widgetLists.get(w);
            if(parent!=null) {
                // Older OptionsRowList rows update Y only; retain their centered columns after resizing.
                var list=lists.stream().filter(l->l.widget==parent).findFirst().orElseThrow(java.util.NoSuchElementException::new);
                int rowDx=listX(list)-list.x+listWidth(list)/2-list.width/2;
                LegacyWidgetLayout.resize(w,box.width,box.height,box.x+rowDx,box.y);
                continue;
            }
            boolean quarterButton=(screen instanceof net.minecraft.client.gui.screen.EditSignScreen
                    || screen instanceof net.minecraft.client.gui.screen.AbstractCommandBlockScreen)&&w instanceof Button
                    && (w.getMessage().getString().equals(net.minecraft.client.gui.DialogTexts.GUI_DONE.getString())
                    || w.getMessage().getString().equals(net.minecraft.client.gui.DialogTexts.GUI_CANCEL.getString()));
            int moveY = quarterButton ? HEIGHT/4-oldHeight/4 : bookScreen() || technicalScreen() ? 0 : screen instanceof GamemodeSelectionScreen ? dy : screen instanceof OptionsScreen ? HEIGHT/6-oldHeight/6 : screen instanceof IngameMenuScreen ? (false ? 0 : pauseDy)
                    : box.y < 33 ? 0 : box.y >= oldHeight - (lists.isEmpty() ? 40 : 64) ? HEIGHT - oldHeight : dy;
            if (screen instanceof IngameMenuScreen && !(false))
                moveY-=pauseGaps.headMap(box.y).values().stream().mapToInt(Integer::intValue).sum();
            boolean fullWidth=w instanceof TextFieldWidget&&box.width>=oldWidth-64;
            int moveX=fullWidth?0:screen instanceof IngameMenuScreen && !(false) ? WIDTH/2-(pauseLeft+pauseRight)/2 : dx;
            int width=fullWidth?Math.max(20,WIDTH-(oldWidth-box.width)):box.width;
            LegacyWidgetLayout.resize(w,width, box.height, box.x + moveX, box.y + moveY);
        }
        for (var box : lists) {
            if(screen instanceof WigoScreen) continue;
            int y = box.y, footer = Math.max(0, oldHeight - box.y - box.height);
            int x = listX(box), width = listWidth(box);
            resizeList(box.widget,width, Math.max(25, HEIGHT - y - footer), x, y);
        }
        
        for (var box : lists) {
            // Owned windows have already positioned their lists for this canvas.
            // Reusing the owner's old top/footer can collapse a centered list to one row.
            if(!(screen instanceof WigoScreen)) {
                int footer = Math.max(0, oldHeight - box.y - box.height);
                resizeList(box.widget,listWidth(box), Math.max(25, HEIGHT - box.y - footer), listX(box), box.y);
            }
            double scroll = box.scroll;
            // Keep off-screen rows reachable without snapping the cursor to the list's
            // center when it crosses the shorter observer viewport's bottom edge.
            if (box.atBottom) scroll = box.widget.getMaxScroll();
            else if (box.height > listHeight(box.widget))
                scroll += Math.max(0, dev.wigo.protocol.LegacyClamp.clamp(mouseY,box.y,box.y+box.height)-box.y-listHeight(box.widget)+8);
            box.widget.setScrollAmount(scroll);
        }
        List<Mapping> mappings = new ArrayList<>();
        // Lists map by row position, rather than stretching the row under the cursor.
        for (var box : lists) if((!(box.widget instanceof LegacySelectionList<?> custom)||custom.visible)&&box.width>0&&box.height>0) mappings.add(new Mapping(box.x, box.y, box.width, box.height,
                listX(box.widget) + (listWidthOf(box.widget) - box.width) / 2f,
                listY(box.widget) + (float)(box.scroll - box.widget.getScrollAmount()), 1, 1));
        for (var box : presentationWidgets) {
            if (!box.visible || box.width<=0 || box.height<=0 || widgetLists.containsKey(box.widget)
                    || screen instanceof IngameMenuScreen && box.widget instanceof INestedGuiEventHandler && !(box.widget instanceof AbstractButton)
                    ||scrollChildren.contains(box.widget)) continue;
            mappings.add(new Mapping(box.x, box.y, box.width, box.height, box.widget.x, box.widget.y,
                    box.widget.getWidth() / (float)Math.max(1, box.width), box.widget.getHeight() / (float)Math.max(1, box.height)));
        }
        pointer = new PointerMap(dx, dy, dev.wigo.compat.Java8.copyList(mappings));
    }
    private NavigableMap<Integer,Integer> omittedPauseRows(List<WidgetBox> controls) {
        NavigableMap<Integer,Integer> gaps=new TreeMap<>();
        if (!(screen instanceof IngameMenuScreen)) return gaps;
        var visible=controls.stream().filter(w->w.visible&&w.width>0&&w.height>0).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList));
        int left=visible.stream().mapToInt(w->w.x).min().orElse(0);
        int right=visible.stream().mapToInt(w->w.x+w.width).max().orElse(0);
        int top=visible.stream().mapToInt(w->w.y).min().orElse(0);
        int bottom=visible.stream().mapToInt(w->w.y+w.height).max().orElse(0);
        // Collapse a complete removed grid row, such as Forge's Mods button.
        // Floating corner buttons and controls beside a vanilla row do not form gaps.
        for (var box:widgets) if (box.visible && box.widget instanceof AbstractButton
                && !GuiCapture.pauseWidget(box.widget) && box.x==left && box.x+box.width==right
                && box.y>top && box.y+box.height<bottom
                && visible.stream().noneMatch(w->box.y<w.y+w.height&&box.y+box.height>w.y))
            gaps.merge(box.y,box.height+4,Math::max);
        return gaps;
    }
    private boolean bookScreen() {
        return screen instanceof EditBookScreen || screen instanceof ReadBookScreen;
    }
    private boolean technicalScreen() {
        return screen instanceof net.minecraft.client.gui.screen.AbstractCommandBlockScreen
                || screen instanceof net.minecraft.client.gui.screen.EditStructureScreen
                || screen instanceof net.minecraft.client.gui.screen.JigsawScreen;
    }
    private boolean columns() { return lists.size()==2 && lists.stream().allMatch(l->l.width<oldWidth-64); }
    private int listWidth(ListBox box) {
        if(screen instanceof WigoScreen) return listWidthOf(box.widget);
        return columns() ? 224 : Math.max(20, WIDTH-(oldWidth-box.width));
    }
    private int listX(ListBox box) {
        if(screen instanceof WigoScreen) return listX(box.widget);
        if (!columns()) return box.x;
        int left=lists.stream().mapToInt(l->l.x).min().orElse(0);
        return (WIDTH-464)/2+(box.x==left?0:240);
    }
    private void collect(IGuiEventListener child, Set<IGuiEventListener> seen) {
        collect(child,seen,null,false);
    }
    private void collect(IGuiEventListener child, Set<IGuiEventListener> seen,AbstractList<?> parent,boolean inScroll) {
        if (!seen.add(child)) return;
        if (child instanceof Widget widget) {widgets.add(new WidgetBox(widget));if(parent!=null)widgetLists.put(widget,parent);if(inScroll)scrollChildren.add(widget);}
        if (child instanceof AbstractList<?> list) {lists.add(new ListBox(list));parent=list;}
        if (child instanceof INestedGuiEventHandler container)
            for (var nested : container.children()) collect(nested, seen,parent,inScroll);
    }
    private void discoverLayouts() {
        // These fields belong to the vanilla screen, not the window or input handler.
        for (Class<?> type = screen.getClass(); type != Screen.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                try {
                    if (field.getType() == int.class && !java.lang.reflect.Modifier.isFinal(field.getModifiers()) && dev.wigo.compat.Java8.set("leftPos", "topPos").contains(field.getName())) {
                        field.setAccessible(true); offsets.put(field, field.getInt(screen));
                    }
                } catch (IllegalAccessException error) { throw new IllegalStateException("Vanilla layout", error); }
            }
        }
    }
    private void set(Field field, int value) {
        try { field.setInt(screen, value); }
        catch (IllegalAccessException error) { throw new IllegalStateException("Vanilla layout", error); }
    }
    @Override public void close() {
        if (!changed) return;
        screen.width = oldWidth; screen.height = oldHeight;
        if(screen instanceof BookLayoutCache cache)cache.wigoClearBookLayout();
        try {
            for (var entry : offsets.entrySet()) set(entry.getKey(), entry.getValue());
            for (var box : lists) resizeList(box.widget,box.width, box.height, box.x, box.y);
            if(nativeScrollLayout)((WigoScreen)screen).layoutWidgets();
            else if(screen instanceof WigoScreen owned)owned.layoutWidgets();
            
            for (var box : lists) {
                resizeList(box.widget,box.width, box.height, box.x, box.y);
                box.widget.setScrollAmount(box.scroll);
                setListHovered(box.widget, box.hovered);
            }
        } finally {
            if(switcher!=null) {switcher.wigoFirstX(firstMouseX);switcher.wigoFirstY(firstMouseY);switcher.wigoFirstSet(firstMouseSet);}
            for (var box : widgets) {
                LegacyWidgetLayout.resize(box.widget,box.width, box.height, box.x, box.y);
                box.widget.visible=box.visible;
                ((WidgetAccessor)box.widget).wigoHovered(box.hovered);
                if(box.selected!=null)((GameModeSlotAccessor)box.widget).wigoSelected(box.selected);
            }
            for(var box:passiveWidgets)((WidgetAccessor)box.widget).wigoHovered(box.hovered);
            changed = false;
        }
    }
    @com.github.bsideup.jabel.Desugar
    public record PointerMap(float dx, float dy, List<Mapping> mappings) {
        public float[] point(float x, float y) {
            for (var m : mappings) if (x >= m.x && x < m.x + m.width && y >= m.y && y < m.y + m.height)
                return m.point(x,y);
            if(mappings.isEmpty())return new float[]{x+dx,y+dy};
            // The same layout anchors describe controls and the empty space between
            // them. A generic center offset would jump at every control boundary.
            NavigableSet<Integer> rows=new TreeSet<>();
            for(var m:mappings){rows.add(m.y);rows.add(m.y+m.height);}
            Integer from=rows.floor((int)Math.floor(y)),to=rows.ceiling((int)Math.ceil(y));
            if(from==null){from=rows.first();to=rows.higher(from);}
            else if(to==null){to=rows.last();from=rows.lower(to);}
            float[] a=row(from,x);
            if(from.equals(to))return a;
            return between(a,row(to,x),(y-from)/(to-from));
        }
        private float[] row(int y,float x) {
            NavigableMap<Integer,float[]> points=new TreeMap<>();
            for(var m:mappings)if(y>=m.y&&y<=m.y+m.height) {
                points.putIfAbsent(m.x,m.point(m.x,y));
                points.putIfAbsent(m.x+m.width,m.point(m.x+m.width,y));
            }
            var from=points.floorEntry((int)Math.floor(x));var to=points.ceilingEntry((int)Math.ceil(x));
            if(from==null){from=points.firstEntry();to=points.higherEntry(from.getKey());}
            else if(to==null){to=points.lastEntry();from=points.lowerEntry(to.getKey());}
            if(from.getKey().equals(to.getKey()))return from.getValue();
            return between(from.getValue(),to.getValue(),(x-from.getKey())/(to.getKey()-from.getKey()));
        }
        private static float[] between(float[] a,float[] b,float t) {
            return new float[]{a[0]+(b[0]-a[0])*t,a[1]+(b[1]-a[1])*t};
        }
    }
    @com.github.bsideup.jabel.Desugar
    public record Mapping(int x, int y, int width, int height, float targetX, float targetY, float scaleX, float scaleY) {
        private float[] point(float x,float y){return new float[]{targetX+(x-this.x)*scaleX,targetY+(y-this.y)*scaleY};}
    }
    @com.github.bsideup.jabel.Desugar
    private record WidgetBox(Widget widget, int x, int y, int width, int height, boolean hovered, Boolean selected, boolean visible) {
        WidgetBox(Widget widget) { this(widget, widget.x, widget.y, widget.getWidth(), widget.getHeight(), ((WidgetAccessor)widget).wigoHovered(),widget instanceof GameModeSlotAccessor slot?slot.wigoSelected():null,widget.visible); }
    }
    private static Object listHovered(AbstractList<?> widget) {
        return null;
    }
    private static void setListHovered(AbstractList<?> widget,Object hovered) {
        
    }
    private void resizeList(AbstractList<?> list,int width,int height,int x,int y) {
        list.updateSize(width,screen.height,y,y+height);list.setLeftPos(x);LegacyListEntry.layout(list);
    }
    private static void setWidgetBounds(Widget widget,int width,int height,int x,int y) {
        widget.setWidth(width);((WidgetAccessor)widget).wigoHeight(height);widget.x=x;widget.y=y;
    }
    private static int listX(AbstractList<?> list) {return ((SelectionListAccessor)list).wigoX();}
    private static int listY(AbstractList<?> list) {return ((SelectionListAccessor)list).wigoY();}
    private static int listWidthOf(AbstractList<?> list) {return ((SelectionListAccessor)list).wigoWidth();}
    private static int listHeight(AbstractList<?> list) {var a=(SelectionListAccessor)list;return a.wigoBottom()-a.wigoY();}
    @com.github.bsideup.jabel.Desugar
    private record ListBox(AbstractList<?> widget, int x, int y, int width, int height, double scroll, boolean atBottom, Object hovered) {
        ListBox(AbstractList<?> widget) { this(widget,listX(widget),listY(widget),listWidthOf(widget),listHeight(widget),widget.getScrollAmount(),widget.getScrollAmount()>0&&widget.getScrollAmount()>=widget.getMaxScroll()-1,listHovered(widget)); }
        boolean contains(float px, float py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }
}
