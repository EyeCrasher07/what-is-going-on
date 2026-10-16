package dev.wigo.mixin;
import dev.wigo.client.GuiCapture;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.model.BookModel;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EnchantmentScreen.class)
public abstract class BookCaptureMixin {
    @Shadow public float open;
    @Shadow public float oOpen;
    @Shadow public float flip;
    @Shadow public float oFlip;
    @Inject(method="renderBg",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/inventory/EnchantmentScreen;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V",ordinal=0,shift=At.Shift.AFTER))
    private void describe(PoseStack pose,float delta,int mouseX,int mouseY,CallbackInfo ci) {
        if(!GuiCapture.recording())return;
        var a=(ContainerScreenAccessor)this;int x=a.wigoLeft(),y=a.wigoTop();
        GuiCapture.record(()->GuiCapture.model(GuiCapture.command("k",null,GuiCapture.clip(),"minecraft:textures/entity/enchanting_table_book.png",GuiCapture.array(x+1,y-1,x+65,y+63),40f,Mth.lerp(delta,oOpen,open),Mth.lerp(delta,oFlip,flip)),new ScreenRectangle(x+1,y-1,64,64)));
    }
    @WrapOperation(method="renderBg",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/BookModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void noGpu(BookModel book,PoseStack pose,VertexConsumer buffer,int light,int overlay,float r,float g,float b,float a,Operation<Void> original) {
        if(!GuiCapture.recording())original.call(book,pose,buffer,light,overlay,r,g,b,a);
    }
}
