package dev.wigo.client;

public final class LegacyCaptureScope implements AutoCloseable {
    private final LegacyGlState saved=new LegacyGlState();
    private final net.minecraft.util.ResourceLocation texture=GuiGraphics.captureTexture;
    public LegacyCaptureScope() {LegacyGlMatrices.begin();}
    public void close() {LegacyGlMatrices.end();saved.close();GuiGraphics.captureTexture=texture;}
}
