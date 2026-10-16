package dev.wigo.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.INestedGuiEventHandler;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import java.util.Map;
import java.util.WeakHashMap;

/** Native pre-1.19.3 tooltips, drawn after controls and included in shared captures. */
public final class LegacyTooltips {
    private static final Map<Widget,ITextComponent> tooltips=new WeakHashMap<>();
    private static final Map<TextFieldWidget,ITextComponent> hints=new WeakHashMap<>();
    public static ITextComponent create(ITextComponent text){return text;}
    public static void set(Widget widget,ITextComponent text){if(text==null)tooltips.remove(widget);else tooltips.put(widget,text);}
    public static ITextComponent get(Widget widget){return tooltips.get(widget);}
    public static void hint(TextFieldWidget widget,ITextComponent text){hints.put(widget,text);updateHints();}
    public static void updateHints(){hints.forEach((widget,text)->widget.setSuggestion(widget.getValue().isEmpty()?text.getString():null));}
    public static void render(Screen screen,MatrixStack pose,int x,int y){render(screen,screen,pose,x,y);}
    private static void render(Screen screen,INestedGuiEventHandler parent,MatrixStack pose,int x,int y) {
        for(IGuiEventListener child:parent.children()) {
            if(child instanceof Widget widget&&widget.visible&&widget.isMouseOver(x,y)&&tooltips.containsKey(widget)) {
                net.minecraft.client.gui.FontRenderer font=net.minecraft.client.Minecraft.getInstance().font;
                java.util.List<net.minecraft.util.IReorderingProcessor> lines=font.split(tooltips.get(widget),Math.max(16,Math.min(170,screen.width-12)));
                if(lines.size()*font.lineHeight+2>screen.height-8)
                    lines=font.split(tooltips.get(widget),Math.max(16,screen.width-12));
                int width=0;for(net.minecraft.util.IReorderingProcessor line:lines)width=Math.max(width,font.width(line));
                int left=x+12;if(left+width>screen.width)left-=28+width;
                left=Math.max(4,Math.min(left,screen.width-width-4));
                // Vanilla's left-side placement can cross the window edge near
                // its midpoint. Supply an anchor whose right-side placement fits.
                screen.renderTooltip(pose,lines,left-12,y);
            }
            if(child instanceof INestedGuiEventHandler nested)render(screen,nested,pose,x,y);
        }
    }
    private LegacyTooltips(){}
}
