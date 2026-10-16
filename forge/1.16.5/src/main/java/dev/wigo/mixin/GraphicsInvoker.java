package dev.wigo.mixin;
import net.minecraft.client.gui.AbstractGui;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(AbstractGui.class)
public interface GraphicsInvoker {}
