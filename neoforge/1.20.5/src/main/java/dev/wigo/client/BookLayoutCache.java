package dev.wigo.client;

/** Older books cache text in screen coordinates; each viewport needs fresh coordinates. */
public interface BookLayoutCache {
    void wigoClearBookLayout();
}
