package dev.wigo.mixin;
import dev.wigo.client.GuiCapture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EnchantmentScreen.class)
public abstract class BookCaptureMixin {
    @Shadow public float open;
    @Shadow public float oOpen;
    @Shadow public float flip;
    @Shadow public float oFlip;
    @Inject(method="renderBook",at=@At("HEAD"),cancellable=true)
    private void wigoBook(GuiGraphics g,int x,int y,float delta,CallbackInfo ci) {
        if(GuiCapture.recording()) {
            GuiCapture.record(()->GuiCapture.model(GuiCapture.command("k",null,GuiCapture.clip(),"minecraft:textures/entity/enchanting_table_book.png",GuiCapture.array(x+1,y-1,x+65,y+63),40f,Mth.lerp(delta,oOpen,open),Mth.lerp(delta,oFlip,flip)),new ScreenRectangle(x+1,y-1,64,64)));
            ci.cancel();
        }
    }
}
