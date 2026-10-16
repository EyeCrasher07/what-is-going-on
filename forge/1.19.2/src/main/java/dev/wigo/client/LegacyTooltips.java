package dev.wigo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Map;
import java.util.WeakHashMap;

/** Native pre-1.19.3 tooltips, drawn after controls and included in shared captures. */
public final class LegacyTooltips {
    private static final Map<AbstractWidget,Component> tooltips=new WeakHashMap<>();
    private static final Map<EditBox,Component> hints=new WeakHashMap<>();
    public static Component create(Component text){return text;}
    public static void set(AbstractWidget widget,Component text){if(text==null)tooltips.remove(widget);else tooltips.put(widget,text);}
    public static Component get(AbstractWidget widget){return tooltips.get(widget);}
    public static void hint(EditBox widget,Component text){hints.put(widget,text);updateHints();}
    public static void updateHints(){hints.forEach((widget,text)->widget.setSuggestion(widget.getValue().isEmpty()?text.getString():null));}
    public static void render(Screen screen,PoseStack pose,int x,int y){render(screen,screen,pose,x,y);}
    private static void render(Screen screen,ContainerEventHandler parent,PoseStack pose,int x,int y) {
        for(GuiEventListener child:parent.children()) {
            if(child instanceof AbstractWidget widget&&widget.visible&&widget.isMouseOver(x,y)&&tooltips.containsKey(widget)) {
                net.minecraft.client.gui.Font font=net.minecraft.client.Minecraft.getInstance().font;
                java.util.List<net.minecraft.util.FormattedCharSequence> lines=font.split(tooltips.get(widget),Math.max(16,Math.min(170,screen.width-12)));
                if(lines.size()*font.lineHeight+2>screen.height-8)
                    lines=font.split(tooltips.get(widget),Math.max(16,screen.width-12));
                int width=0;for(net.minecraft.util.FormattedCharSequence line:lines)width=Math.max(width,font.width(line));
                int left=x+12;if(left+width>screen.width)left-=28+width;
                left=Math.max(4,Math.min(left,screen.width-width-4));
                // Vanilla's left-side placement can cross the window edge near
                // its midpoint. Supply an anchor whose right-side placement fits.
                screen.renderTooltip(pose,lines,left-12,y);
            }
            if(child instanceof ContainerEventHandler nested)render(screen,nested,pose,x,y);
        }
    }
    private LegacyTooltips(){}
}
