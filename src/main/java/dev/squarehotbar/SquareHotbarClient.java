package dev.squarehotbar;

import net.minecraft.client.util.InputUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SquareHotbarClient implements ClientModInitializer {
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("squarehotbar.properties");

    private static float xPosition = 0.195f;
    private static float yPosition = 0.174f;
    private static float sizeScale = 0.087f;

    private static KeyBinding configureKey;

    @Override
    public void onInitializeClient() {
        loadConfig();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("squarehotbar", "general"));
        configureKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.squarehotbar.configure",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configureKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new HotbarConfigScreen());
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof HandledScreen<?> || screen instanceof CreativeInventoryScreen) {
                ScreenEvents.afterRender(screen).register((renderedScreen, graphics, mouseX, mouseY, delta) -> {
                    if (client.player != null) {
                        drawHotbarPanel(client, graphics);
                    }
                });
            }
        });

        HudElementRegistry.addLast(
            Identifier.of("squarehotbar", "hud"),
            (graphics, tickCounter) -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null && client.currentScreen == null) {
                    drawHotbarPanel(client, graphics);
                }
            }
        );
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

    private static void saveConfig() {
        Properties properties = new Properties();
        properties.setProperty("x", Float.toString(xPosition));
        properties.setProperty("y", Float.toString(yPosition));
        properties.setProperty("size", Float.toString(sizeScale));
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (OutputStream output = Files.newOutputStream(CONFIG_FILE)) {
                properties.store(output, "SquareHotbar position and size");
            }
        } catch (Exception ignored) {
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void drawHotbarPanel(MinecraftClient client, DrawContext graphics) {
        if (client.player == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int size = Math.max(24, Math.round(screenHeight * sizeScale));
        int gap = Math.max(1, Math.round(size * 0.067f));
        int x = Math.round(screenWidth * xPosition);
        int y = Math.round(screenHeight * yPosition);
        int total = size * 3 + gap * 2;

        int selectedSlot = client.player.getInventory().getSelectedSlot();
        Identifier slotTexture = Identifier.of("squarehotbar", "textures/gui/slot.png");
        Identifier selectionTexture = Identifier.ofVanilla("hud/hotbar_selection");

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = row * 3 + col;
                int slotX = x + col * (size + gap);
                int slotY = y + row * (size + gap);
                // Draw the user's exact 22x22 slot texture, scaled to the configured slot size.
                graphics.drawTexture(
                    RenderPipelines.GUI_TEXTURED, slotTexture,
                    slotX, slotY, 0.0f, 0.0f, size, size, 22, 22
                );
                if (slot == selectedSlot) {
                    // Use the exact vanilla hotbar selection frame instead of a custom-colored border.
                    graphics.drawGuiTexture(RenderPipelines.GUI_TEXTURED, selectionTexture, slotX - 4, slotY - 4, size + 8, size + 8);
                }

                ItemStack stack = client.player.getInventory().getStack(slot);
                if (!stack.isEmpty()) {
                    // Vanilla hotbar proportions: a 16px item icon inside a 20px slot.
                    // Scale the item and its overlays together so the icon fills 80% of our slot.
                    float itemScale = size / 20.0f;
                    float renderedItemSize = 16.0f * itemScale;
                    float itemX = slotX + (size - renderedItemSize) / 2.0f;
                    float itemY = slotY + (size - renderedItemSize) / 2.0f;

                    graphics.getMatrices().pushMatrix();
                    graphics.getMatrices().translate(itemX, itemY);
                    graphics.getMatrices().scale(itemScale, itemScale);
                    graphics.drawItem(stack, 0, 0);
                    graphics.drawStackOverlay(client.textRenderer, stack, 0, 0);
                    graphics.getMatrices().popMatrix();
                }
            }
        }
    }

    private static final class HotbarConfigScreen extends Screen {
        private HotbarConfigScreen() {
            super(Text.literal("SquareHotbar settings"));
        }

        @Override
        protected void init() {
            int buttonWidth = 82;
            int buttonHeight = 22;
            int gap = 5;
            int rowWidth = buttonWidth * 4 + gap * 3;
            int left = (this.width - rowWidth) / 2;
            int row1 = this.height / 2 + 48;
            int row2 = row1 + buttonHeight + 7;

            addButton(left, row1, buttonWidth, buttonHeight, "← 1 px", () -> xPosition = clamp(xPosition - 1.0f / this.width, 0.0f, 0.85f));
            addButton(left + buttonWidth + gap, row1, buttonWidth, buttonHeight, "→ 1 px", () -> xPosition = clamp(xPosition + 1.0f / this.width, 0.0f, 0.85f));
            addButton(left + (buttonWidth + gap) * 2, row1, buttonWidth, buttonHeight, "↑ 1 px", () -> yPosition = clamp(yPosition - 1.0f / this.height, 0.0f, 0.75f));
            addButton(left + (buttonWidth + gap) * 3, row1, buttonWidth, buttonHeight, "↓ 1 px", () -> yPosition = clamp(yPosition + 1.0f / this.height, 0.0f, 0.75f));

            addButton(left, row2, buttonWidth, buttonHeight, "Размер −", () -> sizeScale = clamp(sizeScale - 0.005f, 0.04f, 0.18f));
            addButton(left + buttonWidth + gap, row2, buttonWidth, buttonHeight, "Размер +", () -> sizeScale = clamp(sizeScale + 0.005f, 0.04f, 0.18f));
            addButton(left + (buttonWidth + gap) * 2, row2, buttonWidth, buttonHeight, "Сброс", () -> {
                xPosition = 0.195f;
                yPosition = 0.174f;
                sizeScale = 0.087f;
            });
            addButton(left + (buttonWidth + gap) * 3, row2, buttonWidth, buttonHeight, "Готово", () -> {
                saveConfig();
                close();
            });
        }

        private void addButton(int x, int y, int width, int height, String label, Runnable action) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> {
                action.run();
                this.clearAndInit();
            }).dimensions(x, y, width, height).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 22, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Настрой положение и размер панели"),
                this.width / 2,
                40,
                0xFFCCCCCC
            );
            drawHotbarPanel(MinecraftClient.getInstance(), context);
            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public void close() {
            MinecraftClient.getInstance().setScreen(null);
        }
    }
}
