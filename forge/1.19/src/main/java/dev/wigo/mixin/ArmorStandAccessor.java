package dev.wigo.mixin;

import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArmorStand.class)
public interface ArmorStandAccessor {
    @Invoker("setShowArms")void wigoShowArms(boolean value);
    @Invoker("setNoBasePlate")void wigoNoBasePlate(boolean value);
}
