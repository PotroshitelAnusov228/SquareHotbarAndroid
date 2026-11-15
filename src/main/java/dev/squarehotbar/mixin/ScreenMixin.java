package dev.squarehotbar.mixin;
import dev.squarehotbar.SquareHotbarClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void squarehotbar$renderOverlay(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        SquareHotbarClient.renderOnScreen((Screen) (Object) this, matrices);
    }
}
