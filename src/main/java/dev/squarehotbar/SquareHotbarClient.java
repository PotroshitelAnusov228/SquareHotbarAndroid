package dev.squarehotbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SquareHotbarClient implements ClientModInitializer {
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
        .getConfigDir().resolve("squarehotbar.properties");

    private static float xPosition = 0.195f;
    private static float yPosition = 0.174f;
    private static float sizeScale = 0.087f;

    @Override
    public void onInitializeClient() {
        loadConfig();
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> renderHotbar(matrices));
    }

    private static void renderHotbar(MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;

        int cell = Math.max(18, Math.min(40, Math.round(client.getWindow().getScaledHeight() * sizeScale)));
        int left = Math.round(client.getWindow().getScaledWidth() * xPosition);
        int top = Math.round(client.getWindow().getScaledHeight() * yPosition);
        int selected = client.player.inventory.selectedSlot;

        for (int slot = 0; slot < 9; slot++) {
            int col = slot % 3;
            int row = slot / 3;
            int x = left + col * cell;
            int y = top + row * cell;

            DrawableHelper.fill(matrices, x, y, x + cell, y + cell, 0xB9000000);
            int border = slot == selected ? 0xFFFFD54A : 0xFF777777;
            DrawableHelper.fill(matrices, x, y, x + cell, y + 1, border);
            DrawableHelper.fill(matrices, x, y + cell - 1, x + cell, y + cell, border);
            DrawableHelper.fill(matrices, x, y, x + 1, y + cell, border);
            DrawableHelper.fill(matrices, x + cell - 1, y, x + cell, y + cell, border);

            ItemStack stack = client.player.inventory.getStack(slot);
            if (!stack.isEmpty()) {
                client.getItemRenderer().renderInGui(matrices, stack, x + (cell - 16) / 2, y + (cell - 16) / 2);
                client.getItemRenderer().renderGuiItemOverlay(
                    matrices, client.textRenderer, stack, x + (cell - 16) / 2, y + (cell - 16) / 2);
            }
        }
    }

    private static void loadConfig() {
        if (!Files.exists(CONFIG_FILE)) return;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(CONFIG_FILE)) {
            properties.load(input);
            xPosition = clamp(parse(properties, "x", xPosition), 0.0f, 0.85f);
            yPosition = clamp(parse(properties, "y", yPosition), 0.0f, 0.75f);
            sizeScale = clamp(parse(properties, "size", sizeScale), 0.04f, 0.18f);
        } catch (Exception ignored) {
        }
    }

    private static float parse(Properties properties, String key, float fallback) {
        try {
            return Float.parseFloat(properties.getProperty(key, Float.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
