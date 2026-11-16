package dev.squarehotbar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SquareHotbarClient implements ClientModInitializer {
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("squarehotbar.properties");
    private static float xPosition = 0.195f, yPosition = 0.174f, sizeScale = 0.087f;
    private static KeyBinding configureKey;

    @Override
    public void onInitializeClient() {
        loadConfig();
        configureKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.squarehotbar.configure", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H,
            "category.squarehotbar.general"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configureKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) client.openScreen(new HotbarConfigScreen());
            }
        });
        // Diagnostic build: HUD render callback intentionally disabled to isolate startup.
    }

    public static void renderOnScreen(Screen screen, MatrixStack matrices) {
        // Rendering is disabled in this diagnostic build.
    }

    private static void loadConfig() {
        if (!Files.exists(CONFIG_FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            p.load(in);
            xPosition = clamp(parse(p, "x", xPosition), 0.0f, 0.85f);
            yPosition = clamp(parse(p, "y", yPosition), 0.0f, 0.75f);
            sizeScale = clamp(parse(p, "size", sizeScale), 0.04f, 0.18f);
        } catch (Exception ignored) {}
    }
    private static float parse(Properties p, String key, float fallback) {
        try { return Float.parseFloat(p.getProperty(key, Float.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }
    private static void saveConfig() {
        Properties p = new Properties();
        p.setProperty("x", Float.toString(xPosition));
        p.setProperty("y", Float.toString(yPosition));
        p.setProperty("size", Float.toString(sizeScale));
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) { p.store(out, "SquareHotbar position and size"); }
        } catch (Exception ignored) {}
    }
    private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }

    private static final class HotbarConfigScreen extends Screen {
        private HotbarConfigScreen() { super(new LiteralText("SquareHotbar settings")); }
        @Override protected void init() {
            int bw = 82, bh = 20, gap = 5, left = (width - (bw * 4 + gap * 3)) / 2;
            int row1 = height / 2 + 48, row2 = row1 + bh + 7;
            addConfigButton(left, row1, bw, bh, "← 1 px", () -> xPosition = clamp(xPosition - 1.0f / width, 0, 0.85f));
            addConfigButton(left + bw + gap, row1, bw, bh, "→ 1 px", () -> xPosition = clamp(xPosition + 1.0f / width, 0, 0.85f));
            addConfigButton(left + (bw + gap) * 2, row1, bw, bh, "↑ 1 px", () -> yPosition = clamp(yPosition - 1.0f / height, 0, 0.75f));
            addConfigButton(left + (bw + gap) * 3, row1, bw, bh, "↓ 1 px", () -> yPosition = clamp(yPosition + 1.0f / height, 0, 0.75f));
            addConfigButton(left, row2, bw, bh, "Размер −", () -> sizeScale = clamp(sizeScale - 0.005f, 0.04f, 0.18f));
            addConfigButton(left + bw + gap, row2, bw, bh, "Размер +", () -> sizeScale = clamp(sizeScale + 0.005f, 0.04f, 0.18f));
            addConfigButton(left + (bw + gap) * 2, row2, bw, bh, "Сброс", () -> { xPosition = 0.195f; yPosition = 0.174f; sizeScale = 0.087f; });
            addConfigButton(left + (bw + gap) * 3, row2, bw, bh, "Готово", () -> { saveConfig(); MinecraftClient.getInstance().openScreen(null); });
        }
        private void addConfigButton(int x, int y, int w, int h, String label, Runnable action) {
            addButton(new ButtonWidget(x, y, w, h, new LiteralText(label), button -> action.run()));
        }
        @Override public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            renderBackground(matrices);
            drawCenteredText(matrices, textRenderer, title, width / 2, 22, 0xFFFFFFFF);
            drawCenteredText(matrices, textRenderer, new LiteralText("HUD rendering disabled for diagnostic test"), width / 2, 40, 0xFFCCCCCC);
            super.render(matrices, mouseX, mouseY, delta);
        }
        @Override public void onClose() { MinecraftClient.getInstance().openScreen(null); }
    }
}