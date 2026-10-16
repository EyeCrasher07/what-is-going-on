package dev.wigo.client;

import dev.wigo.client.GuiGraphics;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.RenderType;

/** Native button with a local icon. Supplied PNGs remain byte-for-byte unchanged. */
final class WigoIconButton extends Button {
    private final ResourceLocation icon;
    private final boolean gear;
    private boolean reset;
    private dev.wigo.config.CursorStyle cursor;
    WigoIconButton(ITextComponent text,IPressable press,ResourceLocation icon,boolean gear){super(0,0,100,20,text,press);this.icon=icon;this.gear=gear;}
    static WigoIconButton resetButton(IPressable press){
        var button=new WigoIconButton(new net.minecraft.util.text.StringTextComponent(""),press,null,false);button.reset=true;return button;
    }
    static WigoIconButton cursorButton(ITextComponent text,IPressable press,dev.wigo.config.CursorStyle cursor){
        var button=new WigoIconButton(text,press,null,false);button.cursor=cursor;return button;
    }
    @Override public void renderButton(com.mojang.blaze3d.matrix.MatrixStack pose,int x,int y,float delta) {var g=GuiGraphics.wrap(pose);
        var caption=getMessage();setMessage(new net.minecraft.util.text.StringTextComponent(""));
        try {super.renderButton(g.pose(),x,y,delta);} finally {setMessage(caption);}
        var font=net.minecraft.client.Minecraft.getInstance().font;
        int labelWidth=font.width(getMessage()),groupWidth=16+(labelWidth>0?4+labelWidth:0);
        int left=this.x+(getWidth()-groupWidth)/2,top=this.y+(getHeight()-16)/2;
        if(labelWidth>0)g.drawCenteredString(font,getMessage(),left+20+labelWidth/2,this.y+(getHeight()-8)/2,
                ((int)Math.ceil(alpha*255)<<24)|(active?0xFFFFFF:0xA0A0A0));
        if(icon!=null)g.blit(icon,left,top,0,0,16,16,16,16);
        if(cursor!=null)CursorSprites.icon(g,cursor,left,top);
        if(gear){
            String[] pixels={"    ####    "," ## #### ## "," ########## ","  ###  ###  ","####    ####","###      ###","###      ###","####    ####","  ###  ###  "," ########## "," ## #### ## ","    ####    "};
            for(int row=0;row<pixels.length;row++)for(int col=0;col<pixels[row].length();col++)if(pixels[row].charAt(col)=='#')g.fill(left+2+col,top+2+row,left+3+col,top+3+row,0xFFE0E0E0);
        }
        if(reset){
            String[] pixels={"   #####    ","  #######   "," ###   ###  "," ##     ##  ","###      ## ","####     ## ","#####    ## ","         ## "," ##     ##  "," ###   ###  ","  #######   ","   #####    "};
            for(int row=0;row<pixels.length;row++)for(int col=0;col<pixels[row].length();col++)if(pixels[row].charAt(col)=='#')g.fill(left+2+col,top+2+row,left+3+col,top+3+row,0xFFE0E0E0);
        }
    }
}
