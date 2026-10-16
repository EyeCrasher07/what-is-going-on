package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.platform.Window;
import dev.wigo.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.function.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import java.util.List;

@Mixin(GuiGraphics.class)
public abstract class GraphicsCaptureMixin {
    private GuiGraphics graphics() {return (GuiGraphics)(Object)this;}
    @WrapMethod(method="renderTooltipInternal")
    private void fitTooltip(Font font,List<ClientTooltipComponent> components,int x,int y,ClientTooltipPositioner positioner,ResourceLocation style,Operation<Void> original) {
        if(GuiCapture.tooltipCanvas()==null||components.isEmpty()) {original.call(font,components,x,y,positioner,style);return;}
        int width=0,height=components.size()==1?-2:0;
        for(var component:components) {width=Math.max(width,component.getWidth(font));height+=component.getHeight(font);}
        var desired=positioner.positionTooltip(graphics().guiWidth(),graphics().guiHeight(),x,y,width,height);
        LegacyTooltipLayout.draw(graphics(),width,height,desired.x(),desired.y(),0,0,
                ()->original.call(font,components,x,y,(ClientTooltipPositioner)(sw,sh,mx,my,w,h)->new org.joml.Vector2i(0,0),style));
    }
    @Inject(method="guiWidth",at=@At("HEAD"),cancellable=true)
    private void width(CallbackInfoReturnable<Integer> ci) {if(GuiViewport.width()!=0)ci.setReturnValue(GuiViewport.width());}
    @Inject(method="guiHeight",at=@At("HEAD"),cancellable=true)
    private void height(CallbackInfoReturnable<Integer> ci) {if(GuiViewport.height()!=0)ci.setReturnValue(GuiViewport.height());}
    @Inject(method="renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",at=@At("HEAD"),cancellable=true)
    private void item(LivingEntity entity,Level level,ItemStack stack,int x,int y,int seed,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->GuiCapture.item(graphics(),stack,x,y,seed));ci.cancel();}
    }
    @Inject(method="innerBlit",at=@At("HEAD"),cancellable=true)
    private void texture(Function<ResourceLocation,RenderType> type,ResourceLocation id,int x0,int x1,int y0,int y1,float u0,float u1,float v0,float v1,int color,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->GuiCapture.texture(graphics(),id,x0,y0,x1,y1,u0,u1,v0,v1,color));ci.cancel();}
    }
    @Inject(method="blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIII)V",at=@At("HEAD"),cancellable=true)
    private void sprite(Function<ResourceLocation,RenderType> type,ResourceLocation id,int x,int y,int w,int h,int color,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->GuiCapture.sprite(graphics(),id,0,0,0,0,x,y,w,h,color));ci.cancel();}
    }
    @Inject(method="blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V",at=@At("HEAD"),cancellable=true)
    private void sprite(Function<ResourceLocation,RenderType> type,ResourceLocation id,int sw,int sh,int u,int v,int x,int y,int w,int h,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->GuiCapture.sprite(graphics(),id,sw,sh,u,v,x,y,w,h,-1));ci.cancel();}
    }
    @Inject(method="blitSprite(Ljava/util/function/Function;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIII)V",at=@At("HEAD"),cancellable=true)
    private void atlas(Function<ResourceLocation,RenderType> type,TextureAtlasSprite sprite,int x,int y,int w,int h,int color,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->GuiCapture.atlas(graphics(),sprite,0,0,0,0,x,y,w,h,color));ci.cancel();}
    }
    @Inject(method="blitSprite(Ljava/util/function/Function;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIIIIIII)V",at=@At("HEAD"),cancellable=true)
    private void atlas(Function<ResourceLocation,RenderType> type,TextureAtlasSprite sprite,int sw,int sh,int u,int v,int x,int y,int w,int h,int color,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->GuiCapture.atlas(graphics(),sprite,sw,sh,u,v,x,y,w,h,color));ci.cancel();}
    }
    @Inject(method="fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V",at=@At("HEAD"),cancellable=true)
    private void rectangle(RenderType type,int x0,int y0,int x1,int y1,int z,int color,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->GuiCapture.rectangle(graphics(),x0,y0,x1,y1,color,color));ci.cancel();}
    }
    @Inject(method="fillGradient(Lnet/minecraft/client/renderer/RenderType;IIIIIII)V",at=@At("HEAD"),cancellable=true)
    private void gradient(RenderType type,int x0,int y0,int x1,int y1,int z,int top,int bottom,CallbackInfo ci) {
        if(GuiCapture.recording()) {GuiCapture.record(()->GuiCapture.rectangle(graphics(),x0,y0,x1,y1,top,bottom));ci.cancel();}
    }
    @Inject(method="enableScissor",at=@At("HEAD"))
    private void scissor(int x0,int y0,int x1,int y1,CallbackInfo ci) {GuiCapture.scissor(graphics(),x0,y0,x1,y1);}
    @Inject(method="disableScissor",at=@At("HEAD"))
    private void scissor(CallbackInfo ci) {GuiCapture.unscissor();}
    @Inject(method={"flush","applyScissor"},at=@At("HEAD"),cancellable=true)
    private void noGpuDuringCapture(CallbackInfo ci) {if(GuiCapture.recording())ci.cancel();}
    @WrapOperation(method="applyScissor",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getHeight()I"))
    private int ownHeight(Window window,Operation<Integer> original) {return GuiScratch.target()==null?original.call(window):GuiScratch.target().height;}
    @WrapOperation(method="applyScissor",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;getGuiScale()D"))
    private double ownScale(Window window,Operation<Double> original) {return GuiScratch.target()==null?original.call(window):(double)GuiScratch.scale();}
    @WrapMethod(method="drawSpecial")
    private void semanticModels(Consumer<MultiBufferSource> operation,Operation<Void> original) {
        if(!GuiCapture.recording()) {original.call(operation);return;}
        if(GuiCapture.active())operation.accept(type->{GuiCapture.fail();return DISCARD;});
    }
    private static final VertexConsumer DISCARD=new VertexConsumer() {
        public VertexConsumer addVertex(float x,float y,float z) {return this;}
        public VertexConsumer setColor(int r,int g,int b,int a) {return this;}
        public VertexConsumer setUv(float u,float v) {return this;}
        public VertexConsumer setUv1(int u,int v) {return this;}
        public VertexConsumer setUv2(int u,int v) {return this;}
        public VertexConsumer setNormal(float x,float y,float z) {return this;}
    };
}
