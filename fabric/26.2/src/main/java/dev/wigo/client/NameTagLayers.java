package dev.wigo.client;

import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import java.util.*;

/** Only the translucent name layer of a player displaying a WIGO panel. Render thread only. */
public final class NameTagLayers {
    private static final Set<Object> marked=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<RenderPipeline,RenderPipeline> pipelines=new IdentityHashMap<>();
    private static final Map<RenderPipeline,RenderPipeline> effectPipelines=new IdentityHashMap<>();
    private static final List<Draw> classic=new ArrayList<>();
    private static boolean preparing;
    private static boolean effect;
    private record Draw(PreparedRenderType type,StagedVertexBuffer.ExecuteInfo info) {}

    public static NameTagFeatureRenderer.Submit mark(NameTagFeatureRenderer.Submit text) {
        var result=new NameTagFeatureRenderer.Submit(new Matrix4f(text.pose()).translate(0,0,0.02f),text.x(),text.y(),text.text(),text.lightCoords(),text.color(),text.backgroundColor(),Font.DisplayMode.NORMAL);
        marked.add(result);return result;
    }
    public static void prepared(NameTagFeatureRenderer.Submit node,Font.PreparedText text) {
        if(marked.remove(node))marked.add(text);
    }
    public static boolean begin(Font.PreparedText content) {
        boolean previous=preparing;preparing=marked.remove(content);return previous;
    }
    public static void end(boolean previous) {preparing=previous;}
    public static boolean preparing() {return preparing;}
    public static int layer() {return preparing?(effect?2:1):0;}
    public static boolean effect(boolean value) {boolean previous=effect;effect=value;return previous;}

    public static PreparedRenderType prepare(PreparedRenderType type) {
        if(!preparing)return type;
        RenderPipeline pipeline=(effect?effectPipelines:pipelines).computeIfAbsent(type.pipeline(),base->{
            var id=base.getLocation();
            return new NamePipeline(base,Identifier.fromNamespaceAndPath("wigo","pipeline/name_tag/"+(effect?"effect/":"glyph/")+id.getNamespace()+"/"+id.getPath()));
        });
        // A translucent background must test depth without blocking water behind its rectangle.
        return new PreparedRenderType(pipeline,type.outputTarget(),type.dynamicTransforms(),type.scissorState(),type.textures());
    }
    public static boolean defer(PreparedRenderType type,StagedVertexBuffer.ExecuteInfo info) {
        if(!type.pipeline().getLocation().getNamespace().equals("wigo")||!type.pipeline().getLocation().getPath().startsWith("pipeline/name_tag/"))return false;
        classic.add(new Draw(type,info));return true;
    }
    public static void renderClassic() {
        // NORMAL consolidates the two vanilla text layers. Draw the dark backdrop
        // first so that moving the see-through layer does not dim the white glyphs.
        classic.sort(Comparator.comparingInt(draw->draw.type.pipeline().getLocation().getPath().contains("/effect/")?0:1));
        try {for(var draw:classic)draw.type.drawFromBuffer(draw.info);}
        finally {classic.clear();}
    }
    public static void endFrame() {marked.clear();classic.clear();preparing=false;effect=false;}
    private static final class NamePipeline extends RenderPipeline {
        NamePipeline(RenderPipeline base,Identifier id) {
            super(id,base.getVertexShader(),base.getFragmentShader(),base.getShaderDefines(),base.getBindGroupLayouts(),
                    base.getColorTargetStates(),new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false),
                    base.getPolygonMode(),base.isCull(),base.getVertexFormatBindings(),
                    base.getPrimitiveTopology(),base.getSortKey());
        }
    }
}
