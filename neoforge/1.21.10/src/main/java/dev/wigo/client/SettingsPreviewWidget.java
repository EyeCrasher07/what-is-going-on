package dev.wigo.client;

import dev.wigo.mixin.PreviewGraphicsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;

/** The view angle belongs to the widget, not to the real player's camera or body. */
public final class SettingsPreviewWidget extends AbstractWidget {
    private float yaw=145, elevation=12, zoom=1;
    private boolean rotating=true, moved;
    private long lastFrame;
    private final PanelLighting lighting=new PanelLighting();

    public SettingsPreviewWidget() {super(0,0,160,180,Component.translatable("wigo.preview.title"));}

    @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float delta) {
        // Sharing F8 never captures a recursively nested 3D preview.
        if(GuiCapture.active())return;
        var mc=Minecraft.getInstance();
        int x=getX(),y=getY(),w=getWidth(),h=getHeight();
        long now=System.nanoTime();
        if(rotating&&lastFrame!=0)yaw=(yaw+(float)Math.min((now-lastFrame)/1_000_000_000.0,0.1)*18)%360;
        lastFrame=now;
        var help=Component.translatable(rotating?"wigo.preview.rotate":"wigo.preview.paused");
        var lines=mc.font.split(help,w-12);
        int footer=lines.size()*10+6;
        for(int i=0;i<lines.size();i++)g.drawString(mc.font,lines.get(i),x+(w-mc.font.width(lines.get(i)))/2,y+h-footer+i*10,0xFFE0E0E0);
        if(mc.player==null) {
            return;
        }
        var player=mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player,1);
        player.nameTag=null;player.shadowPieces.clear();player.outlineColor=0;
        player.lightCoords=15728880;player.isInvisible=false;player.displayFireAnimation=false;
        if(player instanceof LivingEntityRenderState living) {
            living.bodyRot=0;living.yRot=0;living.xRot=0;living.pose=Pose.STANDING;
            living.walkAnimationPos=0;living.walkAnimationSpeed=0;
            living.isInvisibleToPlayer=false;living.deathTime=0;living.isFullyFrozen=false;
        }
        if(player instanceof AvatarRenderState avatar) {
            avatar.scoreText=null;avatar.isSpectator=false;avatar.isCrouching=false;avatar.isFallFlying=false;
            avatar.isVisuallySwimming=false;avatar.isPassenger=false;avatar.isUsingItem=false;
            avatar.swimAmount=0;avatar.attackTime=0;
            avatar.rightArmPose=avatar.leftArmPose=HumanoidModel.ArmPose.EMPTY;
            avatar.rightHandItem.clear();avatar.leftHandItem.clear();avatar.headItem.clear();
            avatar.headEquipment=avatar.chestEquipment=avatar.legsEquipment=avatar.feetEquipment=ItemStack.EMPTY;
        }
        int top=y+2,bottom=y+h-footer-5;
        // Fixed camera scale makes size/height changes visible rather than fitting them away.
        float scale=zoom*Math.min((w-8)/3.6f,Math.max(1,bottom-top)/3.6f);
        int light=mc.level==null?15:PanelRenderer.ambientLight(mc.level,mc.player.blockPosition());
        int brightness=lighting.update(Settings.autoBrightness,Settings.brightness,light,now);
        var state=new SettingsPreviewState(player,yaw,elevation,Settings.transparency,brightness,Settings.size,
                Settings.curvature,Settings.reverseCurvature,Settings.realisticMirroring,Settings.panelOffset,
                Settings.panelPitch,Settings.cursorStyle,x+3,top,x+w-3,bottom,scale,
                new ScreenRectangle(x+3,top,w-6,Math.max(1,bottom-top)));
        ((PreviewGraphicsAccessor)g).wigoPreviewState().submitPicturesInPictureState(state);
    }

    @Override public void onClick(MouseButtonEvent event,boolean doubled) {moved=false;}
    @Override protected void onDrag(MouseButtonEvent event,double dx,double dy) {
        if(dx==0&&dy==0)return;
        moved=true;rotating=false;yaw=(yaw+(float)dx*1.5f)%360;elevation=Math.clamp(elevation+(float)dy,-45,45);
    }
    @Override public void onRelease(MouseButtonEvent event) {if(!moved)rotating=!rotating;lastFrame=0;}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(!isMouseOver(x,y)||vertical==0)return false;
        zoom=Math.clamp(zoom*(float)Math.pow(1.12,vertical),.35f,1.8f);return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(event.isConfirmation()){rotating=!rotating;return true;}
        if(event.isLeft()||event.isRight()){rotating=false;yaw+=event.isLeft()?-10:10;return true;}
        if(event.isUp()||event.isDown()){rotating=false;elevation=Math.clamp(elevation+(event.isUp()?-5:5),-45,45);return true;}
        return super.keyPressed(event);
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {defaultButtonNarrationText(output);}
}
