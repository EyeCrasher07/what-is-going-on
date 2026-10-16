package dev.wigo.mixin;

import net.minecraft.entity.item.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArmorStandEntity.class)
public interface ArmorStandAccessor {
    @Invoker("setShowArms")void wigoShowArms(boolean value);
    @Invoker("setNoBasePlate")void wigoNoBasePlate(boolean value);
}
