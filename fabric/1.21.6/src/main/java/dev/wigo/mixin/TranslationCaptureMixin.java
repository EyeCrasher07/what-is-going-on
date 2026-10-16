package dev.wigo.mixin;

import dev.wigo.client.TextProvenance;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public abstract class TranslationCaptureMixin {
    @Inject(method="getVisualOrder",at=@At("RETURN"))
    private void wigoSource(FormattedText source,CallbackInfoReturnable<FormattedCharSequence> result){TextProvenance.remember(source,result.getReturnValue());}
}
