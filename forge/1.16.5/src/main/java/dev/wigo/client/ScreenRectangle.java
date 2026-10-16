package dev.wigo.client;

/** The small rectangle API used by the wire recorder, independent of vanilla navigation. */
@com.github.bsideup.jabel.Desugar
public record ScreenRectangle(int left, int top, int width, int height) {
    public int right() { return left + width; }
    public int bottom() { return top + height; }
    public ScreenRectangle intersection(ScreenRectangle other) {
        int x = Math.max(left, other.left), y = Math.max(top, other.top);
        int right = Math.min(right(), other.right()), bottom = Math.min(bottom(), other.bottom());
        return right <= x || bottom <= y ? null : new ScreenRectangle(x, y, right - x, bottom - y);
    }
}
