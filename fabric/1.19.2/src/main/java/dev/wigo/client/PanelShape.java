package dev.wigo.client;

/** A cylindrical display, with arc length and height preserved as it bends. */
public final class PanelShape {
    public record Vertex(float x, float y, float z, float u, float v) {}
    private PanelShape() {}
    public static Vertex[] mesh(float halfWidth,float halfHeight,int curvature,boolean mirror,boolean backView) {
        return mesh(halfWidth,halfHeight,curvature,mirror,backView,false);
    }
    public static Vertex[] mesh(float halfWidth,float halfHeight,int curvature,boolean mirror,boolean backView,boolean reverseCurvature) {
        float bend=dev.wigo.protocol.LegacyClamp.clamp(curvature,0,100)/100f;
        int strips=bend==0?1:32;
        float radius=bend==0?Float.POSITIVE_INFINITY:Math.max(0.85f,halfWidth/(float)(Math.PI/3))/bend;
        Vertex[] vertices=new Vertex[strips*8];
        for(int i=0;i<strips;i++) {
            float u0=i/(float)strips,u1=(i+1)/(float)strips;
            float s0=halfWidth*(1-2*u0),s1=halfWidth*(1-2*u1);
            float x0=bend==0?s0:radius*(float)Math.sin(s0/radius),x1=bend==0?s1:radius*(float)Math.sin(s1/radius);
            float direction=reverseCurvature?-1:1;
            float z0=bend==0?0:direction*radius*((float)Math.cos(s0/radius)-1),z1=bend==0?0:direction*radius*((float)Math.cos(s1/radius)-1);
            // An oblique camera can see both face windings of the same cylinder.
            // Choose one image orientation for the whole panel, including its cursor.
            float texture0=backView&&!mirror?1-u0:u0,texture1=backView&&!mirror?1-u1:u1;
            int k=i*8;
            vertices[k]=new Vertex(x0,halfHeight,z0-0.0002f,texture0,1);
            vertices[k+1]=new Vertex(x0,-halfHeight,z0-0.0002f,texture0,0);
            vertices[k+2]=new Vertex(x1,-halfHeight,z1-0.0002f,texture1,0);
            vertices[k+3]=new Vertex(x1,halfHeight,z1-0.0002f,texture1,1);
            vertices[k+4]=new Vertex(x1,halfHeight,z1+0.0002f,texture1,1);
            vertices[k+5]=new Vertex(x1,-halfHeight,z1+0.0002f,texture1,0);
            vertices[k+6]=new Vertex(x0,-halfHeight,z0+0.0002f,texture0,0);
            vertices[k+7]=new Vertex(x0,halfHeight,z0+0.0002f,texture0,1);
        }
        return vertices;
    }
    public static int tint(int transparency) {
        int alpha=Math.round(255*(1-dev.wigo.protocol.LegacyClamp.clamp(transparency,0,100)/100f));
        // Local surfaces contain premultiplied RGB: opacity must attenuate all four channels.
        return alpha*0x01010101;
    }
    /** Opacity uses the alpha byte; RGB carries the 0..200 brightness percentage for the shader. */
    public static int vertexColor(int alpha,int brightness) {
        return dev.wigo.protocol.LegacyClamp.clamp(alpha,0,255)<<24|dev.wigo.protocol.LegacyClamp.clamp(brightness,0,200)*0x00010101;
    }
    /** Older shaders multiply vertex RGB; their global tint supplies only the boost above 100%. */
    public static int legacyVertexColor(int alpha,int brightness) {
        int opacity=dev.wigo.protocol.LegacyClamp.clamp(alpha,0,255),rgb=Math.round(opacity*dev.wigo.protocol.LegacyClamp.clamp(brightness,0,100)/100f);
        return opacity<<24|rgb*0x00010101;
    }
    /** Select one image orientation using the tilted panel's normal, including the camera's height. */
    public static boolean backView(double x,double y,double z,double yaw,double pitch) {
        double around=Math.toRadians(yaw),tilt=Math.toRadians(pitch);
        return Math.cos(tilt)*(-Math.sin(around)*x+Math.cos(around)*z)-Math.sin(tilt)*y>=0;
    }
}
