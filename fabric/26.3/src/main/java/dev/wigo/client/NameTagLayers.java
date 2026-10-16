package dev.wigo.client;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import java.util.*;

/** Only the translucent name layer of a player displaying a WIGO panel. Render thread only. */
public final class NameTagLayers {
    private static final Set<TextFeatureRenderer.Content> marked=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<RenderPipeline,RenderPipeline> pipelines=new IdentityHashMap<>();
    private static final List<Draw> classic=new ArrayList<>();
    private static boolean preparing;
    private record Draw(PreparedRenderType type,StagedVertexBuffer.ExecuteInfo info) {}

    public static TextFeatureRenderer.Submit mark(TextFeatureRenderer.Submit text) {
        marked.add(text.content());
        // Keep the translucent backdrop just in front of the solid glyphs, as in the see-through pass.
        return new TextFeatureRenderer.Submit(new Matrix4f(text.pose()).translate(0,0,0.02f),Font.DisplayMode.NORMAL,text.lightCoords(),text.content());
    }
    public static boolean begin(TextFeatureRenderer.Content content) {
        boolean previous=preparing;preparing=marked.remove(content);return previous;
    }
    public static void end(boolean previous) {preparing=previous;}

    public static PreparedRenderType prepare(PreparedRenderType type) {
        if(!preparing)return type;
        RenderPipeline pipeline=pipelines.computeIfAbsent(type.pipeline(),base->{
            var id=base.getLocation();
            return new NamePipeline(base,Identifier.fromNamespaceAndPath("wigo","pipeline/name_tag/"+id.getNamespace()+"/"+id.getPath()));
        });
        // A translucent background must test depth without blocking water behind its rectangle.
        return new PreparedRenderType("wigo_name_tag/"+type.name(),pipeline,type.oitPipelineSet(),type.dynamicTransforms(),type.scissorState(),type.textures());
    }
    public static boolean defer(PreparedRenderType type,StagedVertexBuffer.ExecuteInfo info) {
        if(!type.name().startsWith("wigo_name_tag/"))return false;
        classic.add(new Draw(type,info));return true;
    }
    public static void renderClassic(RenderPass pass) {
        try {for(var draw:classic)draw.type.drawFromBuffer(draw.info,pass);}
        finally {classic.clear();}
    }
    public static void endFrame() {marked.clear();classic.clear();preparing=false;}
    private static final class NamePipeline extends RenderPipeline {
        NamePipeline(RenderPipeline base,Identifier id) {
            super(id,base.getShaders(),base.getShaderDefines(),base.getBindGroupLayouts(),
                    base.getColorTargetStates().toArray(ColorTargetState[]::new),new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false),
                    base.getPolygonMode(),base.isCull(),base.getVertexFormatBindings().toArray(VertexFormat[]::new),
                    base.getPrimitiveTopology(),base.pushConstantSize(),base.getSortKey());
        }
    }
}
