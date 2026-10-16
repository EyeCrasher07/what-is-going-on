package dev.wigo.client;

public final class PanelShapeTest {
    public static void run() {
        var flat=PanelShape.mesh(1,0.7f,0,false,false);
        check(flat.length==8&&flat[0].x()==1&&flat[2].x()==-1,"Flat display bounds");
        var curved=PanelShape.mesh(1,0.7f,100,false,false);
        check(curved[0].z()<-0.4&&curved[0].x()<flat[0].x(),"Curvature must bend the panel toward the player");
        check(curved[0].y()==flat[0].y()&&Math.abs(curved[curved.length-6].x()+curved[0].x())<1e-5,"Curve must retain height and symmetry");
        var away=PanelShape.mesh(1,0.7f,100,false,false,true);
        check(away[0].z()>0.4&&away[0].x()==curved[0].x()&&away[0].y()==curved[0].y(),"Reverse curve must point away without changing width or height");
        var reverseFlat=PanelShape.mesh(1,0.7f,0,false,false,true);
        check(java.util.Arrays.equals(flat,reverseFlat),"Direction must not change a flat panel");
        for(int i=0;i<curved.length-8;i+=8)check(curved[i+2].x()==curved[i+8].x()&&curved[i+2].u()==curved[i+8].u(),"A seam between display strips");
        var rear=PanelShape.mesh(1,0.7f,0,false,true);
        var mirror=PanelShape.mesh(1,0.7f,0,true,true);
        check(rear[4].u()==0&&mirror[4].u()==1&&flat[0].u()==mirror[0].u(),"Mirroring changes the reverse view only");
        for(int bend:new int[]{0,50,100})for(boolean back:new boolean[]{false,true})for(boolean realistic:new boolean[]{false,true})for(boolean reverse:new boolean[]{false,true}) {
            var mesh=PanelShape.mesh(2.5f,1.5f,bend,realistic,back,reverse);
            for(int i=0;i<mesh.length;i+=8) {
                // Matching points must show the same image even when both face directions are visible.
                for(int j=0;j<4;j++)check(mesh[i+j].u()==mesh[i+7-j].u()&&mesh[i+j].v()==mesh[i+7-j].v(),"A cursor/image is duplicated across curved faces");
            }
            check(mesh[0].u()==(back&&!realistic?1:0),"Whole-panel orientation follows the chosen camera side");
        }
        check(PanelShape.tint(0)==-1&&PanelShape.tint(100)==0&&PanelShape.tint(50)==0x80808080,"Premultiplied opacity");
        for(int light:new int[]{0,50,100,150,200})for(int alpha:new int[]{0,128,255}) {
            int color=PanelShape.vertexColor(alpha,light);
            check((color>>>24)==alpha&&(color&255)==light,"Brightness must not change the opacity byte");
        }
        check(PanelShape.vertexColor(300,300)==0xFFC8C8C8&&PanelShape.vertexColor(-1,-1)==0,"Appearance bounds");
        for(int alpha:new int[]{0,128,255})for(int light:new int[]{0,30,50,100,150,200}) {
            int color=PanelShape.legacyVertexColor(alpha,light);
            check(color>>>24==alpha&&(color&255)==Math.round(alpha*Math.min(light,100)/100f),"Legacy tint changes opacity or premultiplied brightness");
        }
        check(PanelShape.backView(0,0,1,0,0)&&!PanelShape.backView(0,0,-1,0,0),"Default panel side changed");
        check(!PanelShape.backView(0,3,1,0,60)&&PanelShape.backView(0,3,1,0,-60),"Tilted side ignores camera height");
        check(PanelShape.backView(-1,0,0,90,0)&&!PanelShape.backView(1,0,0,90,0),"Side rotation does not follow owner yaw");
        System.out.println("WIGO panel shape passed: cylindrical geometry in both bend directions, seamless UVs, one image across curved faces, camera-side mirroring, independent brightness/opacity");
    }
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
