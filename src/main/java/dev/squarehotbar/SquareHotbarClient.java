package dev.squarehotbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

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
    private static boolean hWasDown;

    @Override
    public void onInitializeClient() {
        loadConfig();

        // Poll H after the client window exists instead of registering a Fabric key binding.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean hDown = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_H)
                == GLFW.GLFW_PRESS;
            if (hDown && !hWasDown && client.player != null && client.currentScreen == null) {
                // Settings screen will be restored after this startup test passes.
            }
            hWasDown = hDown;
        });
    }

    public static void renderOnScreen(Screen screen, MatrixStack matrices) {
        // Rendering remains disabled for this diagnostic build.
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

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
