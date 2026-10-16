package dev.wigo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import dev.wigo.client.ScreenRectangle;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Matrix4f;
import java.util.ArrayDeque;

/** Local drawing facade for the PoseStack GUI used before Minecraft 1.20. */
public final class GuiGraphics {
    public static ResourceLocation captureTexture;
    private final PoseStack pose;
    private final MultiBufferSource.BufferSource buffers;
    private final ArrayDeque<ScreenRectangle> clips=new ArrayDeque<>();
    public GuiGraphics(Minecraft mc,MultiBufferSource.BufferSource buffers) {this(new PoseStack(),buffers);}
    public GuiGraphics(PoseStack pose,MultiBufferSource.BufferSource buffers) {this.pose=pose;this.buffers=buffers;}
    public static GuiGraphics wrap(PoseStack pose) {return new GuiGraphics(pose,Minecraft.getInstance().renderBuffers().bufferSource());}
    public static GuiGraphics matrix(Matrix4f matrix) {var p=new PoseStack();p.last().pose().multiply(matrix);return wrap(p);}
    public PoseStack pose() {return pose;}
    public MultiBufferSource.BufferSource bufferSource() {return buffers;}
    public void flush() {if(!GuiCapture.recording())buffers.endBatch();}
    public void fill(int x0,int y0,int x1,int y1,int color) {flush();GuiComponent.fill(pose,x0,y0,x1,y1,color);}
    public void fillGradient(int x0,int y0,int x1,int y1,int top,int bottom) {
        if(GuiCapture.recording()) {GuiCapture.rectangle(this,x0,y0,x1,y1,top,bottom);return;}
        flush();RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(770,771,1,771);RenderSystem.disableTexture();RenderSystem.shadeModel(org.lwjgl.opengl.GL11.GL_SMOOTH);
        var b=Tesselator.getInstance().getBuilder();b.begin(7,DefaultVertexFormat.POSITION_COLOR);
        var m=pose.last().pose();vertex(b,m,x1,y0,0,top);vertex(b,m,x0,y0,0,top);vertex(b,m,x0,y1,0,bottom);vertex(b,m,x1,y1,0,bottom);
        b.end();BufferUploader.end(b);RenderSystem.disableBlend();RenderSystem.enableTexture();RenderSystem.shadeModel(org.lwjgl.opengl.GL11.GL_FLAT);
    }
    private static void vertex(BufferBuilder b,Matrix4f m,float x,float y,float z,int c) {b.vertex(m,x,y,z).color((c>>16)&255,(c>>8)&255,c&255,c>>>24).endVertex();}
    public int drawString(Font font,String text,int x,int y,int color,boolean shadow) {return drawString(font,new net.minecraft.network.chat.TextComponent(text).getVisualOrderText(),x,y,color,shadow);}
    public int drawString(Font font,Component text,int x,int y,int color,boolean shadow) {return drawString(font,text.getVisualOrderText(),x,y,color,shadow);}
    public int drawString(Font font,FormattedCharSequence text,int x,int y,int color,boolean shadow) {
        int result=font.drawInBatch(text,x,y,color,shadow,pose.last().pose(),buffers,false,0,15728880);
        flush();return result;
    }
    public int drawString(Font f,String t,int x,int y,int c) {return drawString(f,t,x,y,c,true);}
    public int drawString(Font f,Component t,int x,int y,int c) {return drawString(f,t,x,y,c,true);}
    public void drawCenteredString(Font f,Component t,int x,int y,int c) {drawString(f,t,x-f.width(t)/2,y,c,true);}
    public void drawCenteredString(Font f,String t,int x,int y,int c) {drawString(f,t,x-f.width(t)/2,y,c,true);}
    public void renderItem(LivingEntity actor,ItemStack stack,int x,int y,int seed) {flush();LegacyItemDrawing.draw(pose,()->Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(actor,stack,x,y));}
    public void renderItem(ItemStack stack,int x,int y) {renderItem(Minecraft.getInstance().player,stack,x,y,0);}
    public void renderItemDecorations(Font f,ItemStack stack,int x,int y) {LegacyItemDrawing.draw(pose,()->Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(f,stack,x,y));}
    public void blit(ResourceLocation texture,int x,int y,float u,float v,int w,int h,int tw,int th) {texture(texture,x,x+w,y,y+h,u/tw,(u+w)/tw,v/th,(v+h)/th,-1);}
    public void blit(int x,int y,int z,int w,int h,TextureAtlasSprite sprite) {texture(sprite.atlas().location(),x,x+w,y,y+h,sprite.getU0(),sprite.getU1(),sprite.getV0(),sprite.getV1(),-1);}
    public void texture(ResourceLocation id,int x0,int x1,int y0,int y1,float u0,float u1,float v0,float v1,int color) {
        if(GuiCapture.recording()) {GuiCapture.texture(this,id,x0,y0,x1,y1,u0,u1,v0,v1,color);return;}
        flush();RenderSystem.enableTexture();RenderSystem.enableAlphaTest();RenderSystem.alphaFunc(org.lwjgl.opengl.GL11.GL_GREATER,0.1f);Minecraft.getInstance().getTextureManager().bind(id);
        RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(770,771,1,771);RenderSystem.color4f(1,1,1,1);
        var b=Tesselator.getInstance().getBuilder();b.begin(7,DefaultVertexFormat.POSITION_TEX_COLOR);var m=pose.last().pose();
        textured(b,m,x0,y1,u0,v1,color);textured(b,m,x1,y1,u1,v1,color);textured(b,m,x1,y0,u1,v0,color);textured(b,m,x0,y0,u0,v0,color);
        b.end();BufferUploader.end(b);RenderSystem.disableBlend();RenderSystem.enableTexture();RenderSystem.shadeModel(org.lwjgl.opengl.GL11.GL_FLAT);
    }
    private static void textured(BufferBuilder b,Matrix4f m,int x,int y,float u,float v,int c) {b.vertex(m,x,y,0).uv(u,v).color((c>>16)&255,(c>>8)&255,c&255,c>>>24).endVertex();}
    public void enableScissor(int x0,int y0,int x1,int y1) {
        if(GuiCapture.recording()) {GuiCapture.scissor(this,x0,y0,x1,y1);return;}
        flush();
        var r=LegacyBounds.transform(new ScreenRectangle(x0,y0,x1-x0,y1-y0),pose.last().pose());
        if(!clips.isEmpty()) {var a=clips.peek();int l=Math.max(a.left(),r.left()),t=Math.max(a.top(),r.top());r=new ScreenRectangle(l,t,Math.max(0,Math.min(a.right(),r.right())-l),Math.max(0,Math.min(a.bottom(),r.bottom())-t));}
        clips.push(r);applyClip();
    }
    public void disableScissor() {if(GuiCapture.recording()) {GuiCapture.unscissor();return;}flush();if(!clips.isEmpty())clips.pop();applyClip();}
    private void applyClip() {
        if(clips.isEmpty()) {RenderSystem.disableScissor();return;}
        var r=clips.peek();var w=Minecraft.getInstance().getWindow();double s=GuiScratch.target()==null?w.getGuiScale():GuiScratch.scale();int h=GuiScratch.target()==null?w.getHeight():GuiScratch.target().height;
        RenderSystem.enableScissor((int)(r.left()*s),(int)(h-r.bottom()*s),(int)(r.width()*s),(int)(r.height()*s));
    }
}
