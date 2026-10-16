package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.wigo.client.GuiCapture;
import dev.wigo.client.GuiViewport;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix3x2f;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.List;
import java.util.function.Consumer;

@Mixin(GuiGraphics.class)
public abstract class GraphicsCaptureMixin {
    @Unique private boolean wigoFittingTooltip;
    @Inject(method="guiWidth",at=@At("HEAD"),cancellable=true)
    private void virtualWidth(CallbackInfoReturnable<Integer> result) { if (GuiViewport.width()!=0) result.setReturnValue(GuiViewport.width()); }
    @Inject(method="guiHeight",at=@At("HEAD"),cancellable=true)
    private void virtualHeight(CallbackInfoReturnable<Integer> result) { if (GuiViewport.height()!=0) result.setReturnValue(GuiViewport.height()); }
    @WrapMethod(method="renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/ResourceLocation;)V")
    private void tooltip(Font font,List<ClientTooltipComponent> components,int x,int y,ClientTooltipPositioner positioner,ResourceLocation style,Operation<Void> original) {
        wigoFitTooltip(font,components,x,y,positioner,true,adjusted->original.call(font,components,x,y,adjusted,style));
    }
    // Forge and NeoForge defer directly to their overload carrying the item context.
    @WrapMethod(method="renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/world/item/ItemStack;)V",require=0)
    private void itemTooltip(Font font,List<ClientTooltipComponent> components,int x,int y,ClientTooltipPositioner positioner,ResourceLocation style,ItemStack stack,Operation<Void> original) {
        wigoFitTooltip(font,components,x,y,positioner,true,adjusted->original.call(font,components,x,y,adjusted,style,stack));
    }
    @WrapMethod(method="drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V")
    private void centeredComponent(Font font,net.minecraft.network.chat.Component text,int x,int y,int color,Operation<Void> original){
        Float previous=GuiCapture.centered((float)x);try{original.call(font,text,x,y,color);}finally{GuiCapture.centered(previous);}
    }
    @WrapMethod(method="drawCenteredString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V")
    private void centeredString(Font font,String text,int x,int y,int color,Operation<Void> original){
        Float previous=GuiCapture.centered((float)x);try{original.call(font,text,x,y,color);}finally{GuiCapture.centered(previous);}
    }
    @WrapMethod(method="drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V")
    private void centeredSequence(Font font,net.minecraft.util.FormattedCharSequence text,int x,int y,int color,Operation<Void> original){
        Float previous=GuiCapture.centered((float)x);try{original.call(font,text,x,y,color);}finally{GuiCapture.centered(previous);}
    }
    @Unique
    private void wigoFitTooltip(Font font,List<ClientTooltipComponent> components,int x,int y,ClientTooltipPositioner positioner,boolean firstLineGap,Consumer<ClientTooltipPositioner> draw) {
        try(var overlay=GuiCapture.overlay()) {wigoFitTooltipContents(font,components,x,y,positioner,firstLineGap,draw);}
    }
    @Unique
    private void wigoFitTooltipContents(Font font,List<ClientTooltipComponent> components,int x,int y,ClientTooltipPositioner positioner,boolean firstLineGap,Consumer<ClientTooltipPositioner> draw) {
        var canvas=GuiCapture.tooltipCanvas();
        if(canvas==null||components.isEmpty()||wigoFittingTooltip) {draw.accept(positioner);return;}
        var graphics=(GuiGraphics)(Object)this;
        var local=canvas.transformAxisAligned(new Matrix3x2f(graphics.pose()).invert());
        int width=0,height=!firstLineGap||components.size()==1?-2:0;
        for(var component:components) {width=Math.max(width,component.getWidth(font));height+=component.getHeight(font);}
        // Vanilla's tooltip sprites include 3px padding plus a 9px shadow on every side.
        float border=12,scale=Math.min(1,Math.min((local.width()-2f)/(width+2*border),(local.height()-2f)/(height+2*border)));
        var desired=positioner.positionTooltip(graphics.guiWidth(),graphics.guiHeight(),x,y,Math.round(width*scale),Math.round(height*scale));
        float minX=local.left()+1+border*scale,minY=local.top()+1+border*scale;
        float left=Math.clamp((float)desired.x(),minX,Math.max(minX,local.right()-1-(width+border)*scale));
        float top=Math.clamp((float)desired.y(),minY,Math.max(minY,local.bottom()-1-(height+border)*scale));
        graphics.pose().pushMatrix();graphics.pose().translate(left,top).scale(scale,scale);wigoFittingTooltip=true;
        try {draw.accept((sw,sh,mx,my,w,h)->new Vector2i(0,0));}
        finally {wigoFittingTooltip=false;graphics.pose().popMatrix();}
    }
    @Inject(method="renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",at=@At("HEAD"))
    private void item(LivingEntity entity,Level level,ItemStack stack,int x,int y,int seed,CallbackInfo ci) {
        GuiCapture.record(()->GuiCapture.item((GuiGraphics)(Object)this,stack,x,y,seed));
    }
    @WrapMethod(method="innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIIFFFFI)V")
    private void texture(RenderPipeline pipeline,ResourceLocation id,int x0,int x1,int y0,int y1,float u0,float u1,float v0,float v1,int color,Operation<Void> original) {
        GuiCapture.record(()->GuiCapture.texture((GuiGraphics)(Object)this,id,x0,y0,x1,y1,u0,u1,v0,v1,color));
        GuiCapture.imageDepth++;
        try { original.call(pipeline,id,x0,x1,y0,y1,u0,u1,v0,v1,color); } finally { GuiCapture.imageDepth--; }
    }
    @WrapMethod(method="blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIII)V")
    private void sprite(RenderPipeline pipeline,ResourceLocation id,int x,int y,int w,int h,int color,Operation<Void> original) {
        GuiCapture.record(()->GuiCapture.sprite((GuiGraphics)(Object)this,id,0,0,0,0,x,y,w,h,color));
        GuiCapture.imageDepth++;
        try { original.call(pipeline,id,x,y,w,h,color); } finally { GuiCapture.imageDepth--; }
    }
    @WrapMethod(method="blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIIIIIII)V")
    private void croppedSprite(RenderPipeline pipeline,ResourceLocation id,int sw,int sh,int u,int v,int x,int y,int w,int h,int color,Operation<Void> original) {
        GuiCapture.record(()->GuiCapture.sprite((GuiGraphics)(Object)this,id,sw,sh,u,v,x,y,w,h,color));
        GuiCapture.imageDepth++;
        try { original.call(pipeline,id,sw,sh,u,v,x,y,w,h,color); } finally { GuiCapture.imageDepth--; }
    }
    @WrapMethod(method="blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIII)V")
    private void atlas(RenderPipeline pipeline,TextureAtlasSprite sprite,int x,int y,int w,int h,int color,Operation<Void> original) {
        GuiCapture.record(()->GuiCapture.atlas((GuiGraphics)(Object)this,sprite,0,0,0,0,x,y,w,h,color));GuiCapture.imageDepth++;
        try {original.call(pipeline,sprite,x,y,w,h,color);} finally {GuiCapture.imageDepth--;}
    }
    @WrapMethod(method="blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIIIIIII)V")
    private void croppedAtlas(RenderPipeline pipeline,TextureAtlasSprite sprite,int sw,int sh,int u,int v,int x,int y,int w,int h,int color,Operation<Void> original) {
        GuiCapture.record(()->GuiCapture.atlas((GuiGraphics)(Object)this,sprite,sw,sh,u,v,x,y,w,h,color));GuiCapture.imageDepth++;
        try {original.call(pipeline,sprite,sw,sh,u,v,x,y,w,h,color);} finally {GuiCapture.imageDepth--;}
    }
    @WrapMethod(method="submitMapRenderState")
    private void map(MapRenderState state,Operation<Void> original) {
        GuiCapture.record(()->GuiCapture.map((GuiGraphics)(Object)this,state));GuiCapture.muted++;
        try {original.call(state);} finally {GuiCapture.muted--;}
    }
    @Inject(method="getSprite",at=@At("RETURN"))
    private void resolved(Material id,CallbackInfoReturnable<TextureAtlasSprite> cir) { GuiCapture.resolvedSprite(cir.getReturnValue()); }
    @Inject(method="enableScissor",at=@At("HEAD"))
    private void scissor(int x0,int y0,int x1,int y1,CallbackInfo ci) { GuiCapture.scissor((GuiGraphics)(Object)this,x0,y0,x1,y1); }
    @Inject(method="disableScissor",at=@At("HEAD"))
    private void unscissor(CallbackInfo ci) { GuiCapture.unscissor(); }
}
