package dev.squarehotbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
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
    private static KeyBinding configureKey;

    @Override
    public void onInitializeClient() {
        loadConfig();

        configureKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.squarehotbar.configure",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.squarehotbar.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configureKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.openScreen(new HotbarConfigScreen());
                }
            }
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

    private static void saveConfig() {
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

    private static final class HotbarConfigScreen extends Screen {
        private HotbarConfigScreen() {
            super(new LiteralText("SquareHotbar settings"));
        }

        @Override
        protected void init() {
            int buttonWidth = 82;
            int buttonHeight = 20;
            int gap = 4;
            int rowWidth = buttonWidth * 4 + gap * 3;
            int left = (this.width - rowWidth) / 2;
            int row1 = this.height / 2 + 12;
            int row2 = row1 + buttonHeight + 5;

            addButton(new ButtonWidget(left, row1, buttonWidth, buttonHeight, new LiteralText("← 1 px"),
                button -> xPosition = clamp(xPosition - 1.0f / this.width, 0.0f, 0.85f)));
            addButton(new ButtonWidget(left + buttonWidth + gap, row1, buttonWidth, buttonHeight, new LiteralText("→ 1 px"),
                button -> xPosition = clamp(xPosition + 1.0f / this.width, 0.0f, 0.85f)));
            addButton(new ButtonWidget(left + (buttonWidth + gap) * 2, row1, buttonWidth, buttonHeight, new LiteralText("↑ 1 px"),
                button -> yPosition = clamp(yPosition - 1.0f / this.height, 0.0f, 0.75f)));
            addButton(new ButtonWidget(left + (buttonWidth + gap) * 3, row1, buttonWidth, buttonHeight, new LiteralText("↓ 1 px"),
                button -> yPosition = clamp(yPosition + 1.0f / this.height, 0.0f, 0.75f)));

            addButton(new ButtonWidget(left, row2, buttonWidth, buttonHeight, new LiteralText("Size -"),
                button -> sizeScale = clamp(sizeScale - 0.005f, 0.04f, 0.18f)));
            addButton(new ButtonWidget(left + buttonWidth + gap, row2, buttonWidth, buttonHeight, new LiteralText("Size +"),
                button -> sizeScale = clamp(sizeScale + 0.005f, 0.04f, 0.18f)));
            addButton(new ButtonWidget(left + (buttonWidth + gap) * 2, row2, buttonWidth, buttonHeight, new LiteralText("Reset"),
                button -> {
                    xPosition = 0.195f;
                    yPosition = 0.174f;
                    sizeScale = 0.087f;
                }));
            addButton(new ButtonWidget(left + (buttonWidth + gap) * 3, row2, buttonWidth, buttonHeight, new LiteralText("Done"),
                button -> {
                    saveConfig();
                    MinecraftClient.getInstance().openScreen(null);
                }));
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            this.renderBackground(matrices);
            drawCenteredString(matrices, this.textRenderer, this.title.asString(), this.width / 2, 24, 0xFFFFFF);
            drawCenteredString(matrices, this.textRenderer,
                "Adjust the hotbar position and size", this.width / 2, 42, 0xCCCCCC);
            super.render(matrices, mouseX, mouseY, delta);
        }

        @Override
        public void onClose() {
            saveConfig();
            MinecraftClient.getInstance().openScreen(null);
        }
    }
}
