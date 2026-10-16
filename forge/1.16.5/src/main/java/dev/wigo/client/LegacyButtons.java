package dev.wigo.client;

import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;

/** Preserve the current WIGO controls on the older native Button constructor. */
public final class LegacyButtons {
    public static Builder builder(ITextComponent text,Button.IPressable press){return new Builder(text,press);}
    public static final class Builder {
        private final ITextComponent text;private final Button.IPressable press;
        private int x,y,width,height;private ITextComponent tooltip;
        private Builder(ITextComponent text,Button.IPressable press){this.text=text;this.press=press;}
        public Builder bounds(int x,int y,int width,int height){this.x=x;this.y=y;this.width=width;this.height=height;return this;}
        public Builder tooltip(ITextComponent text){tooltip=text;return this;}
        public Button build(){var button=new Button(x,y,width,height,text,press);if(tooltip!=null)LegacyTooltips.set(button,tooltip);return button;}
    }
    private LegacyButtons(){}
}
