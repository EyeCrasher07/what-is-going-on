package dev.wigo.mixin;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(InventoryScreen.class)
public interface RecipeBookAccessor {
    @Accessor("recipeBookComponent") RecipeBookComponent wigoRecipeBook();
}
