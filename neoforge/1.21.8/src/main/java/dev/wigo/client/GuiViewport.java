package dev.wigo.client;

/** Logical bounds for a scratch GUI, including its initial scissor rectangle. */
public final class GuiViewport implements AutoCloseable {
    private static final ThreadLocal<GuiViewport> current = new ThreadLocal<>();
    private final GuiViewport previous;
    private final int width, height;
    private GuiViewport(int width, int height) {
        this.width = width; this.height = height; previous = current.get(); current.set(this);
    }
    public static GuiViewport open(int width, int height) { return new GuiViewport(width, height); }
    public static int width() { return current.get() == null ? 0 : current.get().width; }
    public static int height() { return current.get() == null ? 0 : current.get().height; }
    @Override public void close() { if (previous == null) current.remove(); else current.set(previous); }
}
