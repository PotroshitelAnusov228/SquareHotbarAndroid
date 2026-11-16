package dev.squarehotbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SquareHotbarClient implements ClientModInitializer {
    static final Path CONFIG_FILE = FabricLoader.getInstance()
        .getConfigDir().resolve("squarehotbar.properties");

    static float xPosition = 0.195f;
    static float yPosition = 0.174f;
    static float sizeScale = 0.087f;

    @Override
    public void onInitializeClient() {
        loadConfig();

        // Keep this diagnostic callback empty: no GLFW or window access.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
        });
    }

    public static void renderOnScreen(Screen screen, MatrixStack matrices) {
        // Rendering remains disabled while startup compatibility is being isolated.
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
            // Keep defaults if the config cannot be read.
        }
    }

    private static float parse(Properties properties, String key, float fallback) {
        try {
            return Float.parseFloat(properties.getProperty(key, Float.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static void saveConfig() {
        Properties properties = new Properties();
        properties.setProperty("x", Float.toString(xPosition));
        properties.setProperty("y", Float.toString(yPosition));
        properties.setProperty("size", Float.toString(sizeScale));
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (java.io.OutputStream output = Files.newOutputStream(CONFIG_FILE)) {
                properties.store(output, "SquareHotbar position and size");
            }
        } catch (Exception ignored) {
            // Keep the current settings in memory if saving fails.
        }
    }
}
