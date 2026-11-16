package dev.squarehotbar;

import me.contaria.speedrunapi.config.api.SpeedrunConfigScreenProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public final class SquareHotbarConfigScreenProvider implements SpeedrunConfigScreenProvider {
    @Override
    public Screen createConfigScreen(Screen parent) {
        return new HotbarConfigScreen(parent);
    }

    private static final class HotbarConfigScreen extends Screen {
        private final Screen parent;

        private HotbarConfigScreen(Screen parent) {
            super(new LiteralText("SquareHotbar"));
            this.parent = parent;
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
                button -> SquareHotbarClient.xPosition = SquareHotbarClient.clamp(
                    SquareHotbarClient.xPosition - 1.0f / this.width, 0.0f, 0.85f)));
            addButton(new ButtonWidget(left + buttonWidth + gap, row1, buttonWidth, buttonHeight, new LiteralText("→ 1 px"),
                button -> SquareHotbarClient.xPosition = SquareHotbarClient.clamp(
                    SquareHotbarClient.xPosition + 1.0f / this.width, 0.0f, 0.85f)));
            addButton(new ButtonWidget(left + (buttonWidth + gap) * 2, row1, buttonWidth, buttonHeight, new LiteralText("↑ 1 px"),
                button -> SquareHotbarClient.yPosition = SquareHotbarClient.clamp(
                    SquareHotbarClient.yPosition - 1.0f / this.height, 0.0f, 0.75f)));
            addButton(new ButtonWidget(left + (buttonWidth + gap) * 3, row1, buttonWidth, buttonHeight, new LiteralText("↓ 1 px"),
                button -> SquareHotbarClient.yPosition = SquareHotbarClient.clamp(
                    SquareHotbarClient.yPosition + 1.0f / this.height, 0.0f, 0.75f)));

            addButton(new ButtonWidget(left, row2, buttonWidth, buttonHeight, new LiteralText("Size -"),
                button -> SquareHotbarClient.sizeScale = SquareHotbarClient.clamp(
                    SquareHotbarClient.sizeScale - 0.005f, 0.04f, 0.18f)));
            addButton(new ButtonWidget(left + buttonWidth + gap, row2, buttonWidth, buttonHeight, new LiteralText("Size +"),
                button -> SquareHotbarClient.sizeScale = SquareHotbarClient.clamp(
                    SquareHotbarClient.sizeScale + 0.005f, 0.04f, 0.18f)));
            addButton(new ButtonWidget(left + (buttonWidth + gap) * 2, row2, buttonWidth, buttonHeight, new LiteralText("Reset"),
                button -> {
                    SquareHotbarClient.xPosition = 0.195f;
                    SquareHotbarClient.yPosition = 0.174f;
                    SquareHotbarClient.sizeScale = 0.087f;
                }));
            addButton(new ButtonWidget(left + (buttonWidth + gap) * 3, row2, buttonWidth, buttonHeight, new LiteralText("Done"),
                button -> closeAndSave()));
        }

        private void closeAndSave() {
            SquareHotbarClient.saveConfig();
            MinecraftClient.getInstance().openScreen(parent);
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            renderBackground(matrices);
            drawCenteredString(matrices, textRenderer, "SquareHotbar settings", width / 2, 24, 0xFFFFFF);
            drawCenteredString(matrices, textRenderer, "Adjust position and size", width / 2, 42, 0xCCCCCC);
            super.render(matrices, mouseX, mouseY, delta);
        }

        @Override
        public void onClose() {
            closeAndSave();
        }
    }
}
