package dev.wigo.client;

import net.minecraft.client.gui.components.AbstractWidget;

public final class LegacyWidgetLayout {
    public static void resize(AbstractWidget widget,int width,int height,int x,int y) {
        widget.setWidth(width);widget.setHeight(height);widget.setX(x);widget.setY(y);
    }
    private LegacyWidgetLayout() {}
}
