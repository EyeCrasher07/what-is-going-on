package dev.wigo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wigo.client.NameTagLayers;
import dev.wigo.client.PanelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;

@Mixin(NameTagFeatureRenderer.Storage.class)
public abstract class NameTagDepthMixin {
    @WrapOperation(method="add",at=@At(value="INVOKE",target="Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean wigoMarkName(List<Object> list,Object node,Operation<Boolean> original) {
        if(PanelRenderer.nameTagsNeedDepthTest()&&node instanceof SubmitNodeStorage.NameTagSubmit name)NameTagLayers.mark(name);
        return original.call(list,node);
    }
}
