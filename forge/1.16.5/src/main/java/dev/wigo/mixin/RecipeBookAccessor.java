package dev.wigo.mixin;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.recipebook.RecipeBookGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(InventoryScreen.class)
public interface RecipeBookAccessor {
    @Accessor("recipeBookComponent") RecipeBookGui wigoRecipeBook();
}
