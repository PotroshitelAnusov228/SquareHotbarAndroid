package dev.squarehotbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public final class SquareHotbarClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HudElementRegistry.addLast(
            Identifier.of("squarehotbar", "hud"),
            (graphics, tickCounter) -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player == null) return;

                int size = 34;
                int gap = 3;
                int total = size * 3 + gap * 2;

                int x = 12;
                int y = 12;
                int buttonY = y + total + 10;
                int buttonHeight = 24;

                if (client.currentScreen == null) {
                    double mouseX = client.mouse.getScaledX(client.getWindow());
                    double mouseY = client.mouse.getScaledY(client.getWindow());

                    boolean overPanel =
                        mouseX >= x - 4 && mouseX <= x + total + 4 &&
                        mouseY >= y - 4 && mouseY <= buttonY + buttonHeight;

                    if (overPanel && client.mouse.wasLeftButtonClicked()) {
                        if (mouseY >= buttonY && mouseY <= buttonY + buttonHeight
                                && mouseX >= x && mouseX <= x + total) {
                            client.setScreen(new InventoryScreen(client.player));
                            return;
                        }

                        for (int row = 0; row < 3; row++) {
                            for (int col = 0; col < 3; col++) {
                                int slotX = x + col * (size + gap);
                                int slotY = y + row * (size + gap);

                                if (mouseX >= slotX && mouseX < slotX + size
                                        && mouseY >= slotY && mouseY < slotY + size) {
                                    client.player.getInventory().setSelectedSlot(row * 3 + col);
                                }
                            }
                        }
                    }
                }

                graphics.fill(
                    x - 4, y - 4,
                    x + total + 4, y + total + 4,
                    0xAA151515
                );

                int selectedSlot = client.player.getInventory().getSelectedSlot();

                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        int slot = row * 3 + col;
                        int slotX = x + col * (size + gap);
                        int slotY = y + row * (size + gap);

                        int borderColor = slot == selectedSlot ? 0xFFFFC857 : 0xFF555555;
                        graphics.fill(
                            slotX, slotY,
                            slotX + size, slotY + size,
                            borderColor
                        );

                        graphics.fill(
                            slotX + 2, slotY + 2,
                            slotX + size - 2, slotY + size - 2,
                            0xFF252525
                        );

                        ItemStack stack = client.player.getInventory().getStack(slot);
                        if (!stack.isEmpty()) {
                            int itemX = slotX + (size - 16) / 2;
                            int itemY = slotY + (size - 16) / 2;
                            graphics.drawItem(stack, itemX, itemY);
                            graphics.drawStackOverlay(client.textRenderer, stack, itemX, itemY);
                        }
                    }
                }

                graphics.fill(x, buttonY, x + total, buttonY + buttonHeight, 0xFF555555);
                graphics.fill(x + 2, buttonY + 2, x + total - 2, buttonY + buttonHeight - 2, 0xFF252525);
                graphics.drawText(
                    client.textRenderer,
                    "INVENTORY",
                    x + 8,
                    buttonY + 8,
                    0xFFFFFFFF,
                    true
                );
            }
        );
    }
}