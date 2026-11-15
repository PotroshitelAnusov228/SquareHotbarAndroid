package dev.squarehotbar;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SquareHotbarClient implements ClientModInitializer {
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("squarehotbar.properties");
    private static final Identifier SLOT_TEXTURE = new Identifier("squarehotbar", "textures/gui/slot.png");
    private static final Identifier WIDGETS_TEXTURE = new Identifier("minecraft", "textures/gui/widgets.png");
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
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.currentScreen == null) drawHotbarPanel(client, matrices);
        });
    }

    public static void renderOnScreen(Screen screen, MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && !(screen instanceof HotbarConfigScreen)) drawHotbarPanel(client, matrices);
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

    private static void drawHotbarPanel(MinecraftClient client, MatrixStack matrices) {
        if (client.player == null) return;
        int sw = client.getWindow().getScaledWidth(), sh = client.getWindow().getScaledHeight();
        int size = Math.max(24, Math.round(sh * sizeScale));
        int total = size * 3 + Math.max(1, Math.round(size * 0.067f)) * 2;
        int x = Math.round(sw * xPosition), y = Math.round(sh * yPosition);
        int selected = client.player.inventory.selectedSlot;

        client.getTextureManager().bindTexture(SLOT_TEXTURE);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            int sx = x + col * total / 3, sy = y + row * total / 3;
            int w = x + (col + 1) * total / 3 - sx, h = y + (row + 1) * total / 3 - sy;
            DrawableHelper.drawTexture(matrices, sx, sy, 0, 0.0f, 0.0f, w, h, 22, 22);
        }

        int col = selected % 3, row = selected / 3;
        int sx = x + col * total / 3, sy = y + row * total / 3;
        int w = x + (col + 1) * total / 3 - sx, h = y + (row + 1) * total / 3 - sy;
        client.getTextureManager().bindTexture(WIDGETS_TEXTURE);
        DrawableHelper.drawTexture(matrices, sx - 4, sy - 4, 0, 0.0f, 22.0f, w + 8, h + 8, 256, 256);

        for (row = 0; row < 3; row++) for (col = 0; col < 3; col++) {
            int slot = row * 3 + col;
            sx = x + col * total / 3; sy = y + row * total / 3;
            w = x + (col + 1) * total / 3 - sx; h = y + (row + 1) * total / 3 - sy;
            ItemStack stack = client.player.inventory.getStack(slot);
            if (!stack.isEmpty()) {
                float scale = Math.min(w, h) / 20.0f;
                float rendered = 16.0f * scale;
                float ix = sx + (w - rendered) / 2.0f, iy = sy + (h - rendered) / 2.0f;
                RenderSystem.pushMatrix();
                RenderSystem.translatef(ix, iy, 0.0f);
                RenderSystem.scalef(scale, scale, 1.0f);
                client.getItemRenderer().renderInGuiWithOverrides(stack, 0, 0);
                client.getItemRenderer().renderGuiItemOverlay(client.textRenderer, stack, 0, 0);
                RenderSystem.popMatrix();
            }
        }
    }

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
            drawCenteredText(matrices, textRenderer, new LiteralText("Настрой положение и размер панели"), width / 2, 40, 0xFFCCCCCC);
            drawHotbarPanel(MinecraftClient.getInstance(), matrices);
            super.render(matrices, mouseX, mouseY, delta);
        }
        @Override public void onClose() { MinecraftClient.getInstance().openScreen(null); }
    }
}
