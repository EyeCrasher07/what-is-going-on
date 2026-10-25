package dev.wigo.client;

import net.minecraft.client.gui.screens.Screen;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.WeakHashMap;

/** Recognize inherited vanilla screens and keep added controls out of their presentation. */
public final class VanillaGui {
    private static final Map<Object,Boolean> added=new WeakHashMap<>();
    private VanillaGui() {}
    public static boolean knownScreen(Screen screen) {
        for(Class<?> type=screen.getClass();type!=null&&type!=Screen.class;type=type.getSuperclass())
            if(type.getName().startsWith("net.minecraft.")&&!Modifier.isAbstract(type.getModifiers()))return true;
        return false;
    }
    public static void widgetAdded(Screen screen,Object widget) {
        if(widget==null||WigoLinks.owns(screen))return;
        String base=Screen.class.getName();
        for(StackTraceElement frame:Thread.currentThread().getStackTrace()) {
            String name=frame.getClassName();
            if(name.startsWith("java.")||name.startsWith("dev.wigo.")||name.equals(base))continue;
            // Mixin event handlers can live inside a vanilla class after transformation.
            String method=frame.getMethodName();
            added.put(widget,!name.startsWith("net.minecraft.")||method.startsWith("handler$")
                    ||method.startsWith("redirect$")||method.startsWith("wrapOperation$"));
            return;
        }
    }
    public static boolean addedWidget(Object widget) {
        return !widget.getClass().getName().startsWith("net.minecraft.")||Boolean.TRUE.equals(added.get(widget));
    }
    public static boolean overAddedControl(Screen screen,double x,double y) {
        for(var child:screen.children())if(GuiCapture.decoration(screen,child)&&child.isMouseOver(x,y))return true;
        return false;
    }
    public static boolean nativeDrawing(Class<?> graphics) {
        // Inspect only unowned draw calls, during the requested capture pass.
        // Find the caller beyond the graphics facade, including Mixin wrappers.
        StackTraceElement[] frames=Thread.currentThread().getStackTrace();
        int caller=-1;String facade=graphics.getName();
        for(int i=0;i<frames.length;i++)if(frames[i].getClassName().equals(facade))caller=i+1;
        if(caller<0)return true;
        for(int i=caller;i<frames.length;i++) {
            String name=frames[i].getClassName();
            if(name.startsWith("java.")||name.startsWith("dev.wigo.")||name.startsWith("com.llamalad7."))continue;
            String method=frames[i].getMethodName();
            if((method.startsWith("handler$")||method.startsWith("redirect$")||method.startsWith("wrapOperation$"))
                    &&!method.contains("wigo"))return false;
            return name.startsWith("net.minecraft.")||name.startsWith("net.optifine.");
        }
        return true;
    }
}
