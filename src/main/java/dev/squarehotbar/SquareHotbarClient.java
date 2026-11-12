package dev.squarehotbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public final class SquareHotbarClient implements ClientModInitializer {

    private static final int SIZE = 34;
    private static final int GAP = 3;
    private static final int X = 12;
    private static final int Y = 12;
    private static final int TOTAL = SIZE * 3 + GAP * 2;
    private static final int BUTTON_Y = Y + TOTAL + 10;
    private static final int BUTTON_HEIGHT = 24;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> handleClick(client));

        HudElementRegistry.addLast(
            Identifier.of("squarehotbar", "hud"),
            (graphics, tickCounter) -> renderHud(MinecraftClient.getInstance(), graphics)
        );
    }

    private static void handleClick(MinecraftClient client) {
        if (client.player == null || client.currentScreen != null) return;
        if (!client.mouse.wasLeftButtonClicked()) return;

        double mouseX = client.mouse.getScaledX(client.getWindow());
        double mouseY = client.mouse.getScaledY(client.getWindow());

        if (mouseX >= X && mouseX <= X + TOTAL
                && mouseY >= BUTTON_Y && mouseY <= BUTTON_Y + BUTTON_HEIGHT) {
            client.setScreen(new InventoryScreen(client.player));
            return;
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = X + col * (SIZE + GAP);
                int slotY = Y + row * (SIZE + GAP);

                if (mouseX >= slotX && mouseX < slotX + SIZE
                        && mouseY >= slotY && mouseY < slotY + SIZE) {
                    client.player.getInventory().setSelectedSlot(row * 3 + col);
                    return;
                }
            }
        }
    }

    private static void renderHud(MinecraftClient client, net.minecraft.client.gui.DrawContext graphics) {
        if (client.player == null) return;

        graphics.fill(
            X - 4, Y - 4,
            X + TOTAL + 4, Y + TOTAL + 4,
            0xAA151515
        );

        int selectedSlot = client.player.getInventory().getSelectedSlot();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = row * 3 + col;
                int slotX = X + col * (SIZE + GAP);
                int slotY = Y + row * (SIZE + GAP);

                int borderColor = slot == selectedSlot ? 0xFFFFC857 : 0xFF555555;
                graphics.fill(
                    slotX, slotY,
                    slotX + SIZE, slotY + SIZE,
                    borderColor
                );

                graphics.fill(
                    slotX + 2, slotY + 2,
                    slotX + SIZE - 2, slotY + SIZE - 2,
                    0xFF252525
                );

                ItemStack stack = client.player.getInventory().getStack(slot);
                if (!stack.isEmpty()) {
                    int itemX = slotX + (SIZE - 16) / 2;
                    int itemY = slotY + (SIZE - 16) / 2;
                    graphics.drawItem(stack, itemX, itemY);
                    graphics.drawStackOverlay(client.textRenderer, stack, itemX, itemY);
                }
            }
        }

        graphics.fill(X, BUTTON_Y, X + TOTAL, BUTTON_Y + BUTTON_HEIGHT, 0xFF555555);
        graphics.fill(X + 2, BUTTON_Y + 2, X + TOTAL - 2, BUTTON_Y + BUTTON_HEIGHT - 2, 0xFF252525);
        graphics.drawText(
            client.textRenderer,
            "INVENTORY",
            X + 8,
            BUTTON_Y + 8,
            0xFFFFFFFF,
            true
        );
    }
}