package dev.wigo.client;

/** Eye stays at the anchor; the number flips only when its preferred side would clip. */
public record ObserverBadgeLayout(int eyeX,int y,int numberX,int left,int right,boolean numberLeft) {
    public static final int EYE_WIDTH=13,HEIGHT=9,GAP=3,PADDING=3;
    public static ObserverBadgeLayout at(int width,int height,double anchorX,double anchorY,int numberWidth) {
        int x=Math.clamp((int)Math.round(anchorX)-EYE_WIDTH/2,PADDING,Math.max(PADDING,width-PADDING-EYE_WIDTH));
        int y=Math.clamp((int)Math.round(anchorY)-HEIGHT/2,PADDING,Math.max(PADDING,height-PADDING-HEIGHT));
        boolean left=anchorX<width/2.0;
        if(left&&x-GAP-numberWidth<PADDING)left=false;
        else if(!left&&x+EYE_WIDTH+GAP+numberWidth>width-PADDING)left=true;
        int number=left?x-GAP-numberWidth:x+EYE_WIDTH+GAP;
        return new ObserverBadgeLayout(x,y,number,Math.min(x,number),Math.max(x+EYE_WIDTH,number+numberWidth),left);
    }
}
