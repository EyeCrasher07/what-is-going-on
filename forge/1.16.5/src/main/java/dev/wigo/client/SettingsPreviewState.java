package dev.wigo.client;
import net.minecraft.entity.LivingEntity;
import dev.wigo.config.CursorStyle;
import dev.wigo.client.ScreenRectangle;
/** Local-only sample state; never enters the network mirror. */
@com.github.bsideup.jabel.Desugar
public record SettingsPreviewState(LivingEntity player,float yaw,float elevation,int transparency,int brightness,
        int size,int curvature,boolean reverse,boolean mirror,int offset,int pitch,CursorStyle cursor,
        int x0,int y0,int x1,int y1,float scale,ScreenRectangle scissorArea) {}
