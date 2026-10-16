package dev.wigo.mixin;

import dev.wigo.client.TextProvenance;
import net.minecraft.client.resources.ClientLanguageMap;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.IReorderingProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguageMap.class)
public abstract class TranslationCaptureMixin {
    @Inject(method="getVisualOrder",at=@At("RETURN"))
    private void wigoSource(ITextProperties source,CallbackInfoReturnable<IReorderingProcessor> result){TextProvenance.remember(source,result.getReturnValue());}
}
