package dev.squarehotbar;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public final class SquareHotbarClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Diagnostic build: intentionally empty client initializer.
    }

    // Kept for compatibility with ScreenMixin; intentionally performs no rendering.
    public static void renderOnScreen(Screen screen, MatrixStack matrices) {
        // no-op
    }
}
