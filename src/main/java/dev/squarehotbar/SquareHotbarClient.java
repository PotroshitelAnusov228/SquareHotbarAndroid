package dev.squarehotbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public final class SquareHotbarClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InventoryScreen) {
                ScreenEvents.afterRender(screen).register((renderedScreen, graphics, mouseX, mouseY, delta) ->
                    drawHotbarPanel(client, graphics)
                );
            }
        });

        HudElementRegistry.addLast(
            Identifier.of("squarehotbar", "hud"),
            (graphics, tickCounter) -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player == null) return;
                if (client.currentScreen == null) {
                    drawHotbarPanel(client, graphics);
                }
            }
        );
    }

    private static void drawHotbarPanel(MinecraftClient client, DrawContext graphics) {
        if (client.player == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Match the 3x3 Zalith touch-button grid from the user's layout.
        int size = Math.max(24, Math.round(screenHeight * 0.087f));
        int gap = Math.max(1, Math.round(size * 0.067f));
        int x = Math.round(screenWidth * 0.195f);
        int y = Math.round(screenHeight * 0.174f);
        int total = size * 3 + gap * 2;

        graphics.fill(x - 3, y - 3, x + total + 3, y + total + 3, 0xAA151515);
        int selectedSlot = client.player.getInventory().getSelectedSlot();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = row * 3 + col;
                int slotX = x + col * (size + gap);
                int slotY = y + row * (size + gap);
                int borderColor = slot == selectedSlot ? 0xFFFFC857 : 0xFF555555;

                graphics.fill(slotX, slotY, slotX + size, slotY + size, borderColor);
                graphics.fill(slotX + 2, slotY + 2, slotX + size - 2, slotY + size - 2, 0xFF252525);

                ItemStack stack = client.player.getInventory().getStack(slot);
                if (!stack.isEmpty()) {
                    int itemX = slotX + (size - 16) / 2;
                    int itemY = slotY + (size - 16) / 2;
                    graphics.drawItem(stack, itemX, itemY);
                    graphics.drawStackOverlay(client.textRenderer, stack, itemX, itemY);
                }
            }
        }
    }
}
