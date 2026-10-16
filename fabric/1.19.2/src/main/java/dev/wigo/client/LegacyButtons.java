package dev.wigo.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Preserve the current WIGO controls on the older native Button constructor. */
public final class LegacyButtons {
    public static Builder builder(Component text,Button.OnPress press){return new Builder(text,press);}
    public static final class Builder {
        private final Component text;private final Button.OnPress press;
        private int x,y,width,height;private Component tooltip;
        private Builder(Component text,Button.OnPress press){this.text=text;this.press=press;}
        public Builder bounds(int x,int y,int width,int height){this.x=x;this.y=y;this.width=width;this.height=height;return this;}
        public Builder tooltip(Component text){tooltip=text;return this;}
        public Button build(){var button=new Button(x,y,width,height,text,press);if(tooltip!=null)LegacyTooltips.set(button,tooltip);return button;}
    }
    private LegacyButtons(){}
}
