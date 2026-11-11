ackage dev.squarehotbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public final class SquareHotbarClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("squarehotbar", "hud"),
            (graphics, tickCounter) -> {
                int size = 34;
                int gap = 3;
                int total = size * 3 + gap * 2;

                int x = 12;
                int y = 12;

                // Фон панели
                graphics.fill(
                    x - 4, y - 4,
                    x + total + 4, y + total + 4,
                    0xAA151515
                );

                // Девять квадратных слотов
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        int slotX = x + col * (size + gap);
                        int slotY = y + row * (size + gap);

                        graphics.fill(
                            slotX, slotY,
                            slotX + size, slotY + size,
                            0xFF555555
                        );

                        graphics.fill
