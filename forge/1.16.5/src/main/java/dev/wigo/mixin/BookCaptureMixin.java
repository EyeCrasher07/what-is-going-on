package dev.wigo.mixin;
import dev.wigo.client.GuiCapture;
import dev.wigo.client.ScreenRectangle;
import net.minecraft.client.gui.screen.EnchantmentScreen;
import net.minecraft.client.renderer.entity.model.BookModel;
import net.minecraft.util.math.MathHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
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
    @Inject(method="renderBg",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screen/EnchantmentScreen;blit(Lcom/mojang/blaze3d/matrix/MatrixStack;IIIIII)V",ordinal=0,shift=At.Shift.AFTER))
    private void describe(MatrixStack pose,float delta,int mouseX,int mouseY,CallbackInfo ci) {
        if(!GuiCapture.recording())return;
        var a=(ContainerScreenAccessor)this;int x=a.wigoLeft(),y=a.wigoTop();
        GuiCapture.record(()->GuiCapture.model(GuiCapture.command("k",null,GuiCapture.clip(),"minecraft:textures/entity/enchanting_table_book.png",GuiCapture.array(x+1,y-1,x+65,y+63),40f,MathHelper.lerp(delta,oOpen,open),MathHelper.lerp(delta,oFlip,flip)),new ScreenRectangle(x+1,y-1,64,64)));
    }
    @WrapOperation(method="renderBg",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/model/BookModel;renderToBuffer(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;IIFFFF)V"))
    private void noGpu(BookModel book,MatrixStack pose,IVertexBuilder buffer,int light,int overlay,float r,float g,float b,float a,Operation<Void> original) {
        if(!GuiCapture.recording())original.call(book,pose,buffer,light,overlay,r,g,b,a);
    }
}
