package dev.wigo.client;

import dev.wigo.mixin.WidgetAccessor;
import net.minecraft.client.gui.components.AbstractWidget;

public final class LegacyWidgetLayout {
    public static void resize(AbstractWidget widget,int width,int height,int x,int y) {
        widget.setWidth(width);((WidgetAccessor)widget).wigoHeight(height);widget.x=x;widget.y=y;
    }
    private LegacyWidgetLayout() {}
}
