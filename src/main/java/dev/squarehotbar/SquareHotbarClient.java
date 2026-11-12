package dev.squarehotbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SquareHotbarClient implements ClientModInitializer {

    private static final int SIZE = 34;
    private static final int GAP = 3;
    private static final int X = 12;
    private static final int Y = 12;
    private static final int TOTAL = SIZE * 3 + GAP * 2;
    private static final int BUTTON_Y = Y + TOTAL + 10;
    private static final int BUTTON_HEIGHT = 24;

    private static final Set<Integer> ACTIVE_TOUCH_POINTERS = new HashSet<>();
    private static Field touchStateModelField;
    private static Object renderEventsInstance;
    private static Field pointerStateField;
    private static Object invalidPointerState;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(SquareHotbarClient::handleMouseClick);
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InventoryScreen) {
                ScreenEvents.afterRender(screen).register((renderedScreen, graphics, mouseX, mouseY, delta) ->
                    drawHotbarPanel(client, graphics, true)
                );
                Screens.getButtons(screen).add(
                    ButtonWidget.builder(Text.literal("CLOSE"), button -> client.setScreen(null))
                        .dimensions(X, BUTTON_Y, TOTAL, BUTTON_HEIGHT)
                        .build()
                );
            }
        });
        HudElementRegistry.addLast(
            Identifier.of("squarehotbar", "hud"),
            (graphics, tickCounter) -> renderHud(MinecraftClient.getInstance(), graphics)
        );
    }

    private static void handleMouseClick(MinecraftClient client) {
        if (client.player == null || client.currentScreen != null) return;
        if (!client.mouse.wasLeftButtonClicked()) return;
        double mouseX = client.mouse.getScaledX(client.getWindow());
        double mouseY = client.mouse.getScaledY(client.getWindow());
        handleTap(client, mouseX, mouseY);
    }

    private static void handleTouchControllerPointers(MinecraftClient client) {
        try {
            Class<?> renderEvents = Class.forName(
                "top.fifthlight.touchcontroller.common.event.render.RenderEvents"
            );

            if (renderEventsInstance == null) {
                Field instanceField = renderEvents.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                renderEventsInstance = instanceField.get(null);
            }

            if (touchStateModelField == null) {
                touchStateModelField = renderEvents.getDeclaredField("touchStateModel");
                touchStateModelField.setAccessible(true);
            }

            Object touchStateModel = touchStateModelField.get(renderEventsInstance);
            Method getPointers = touchStateModel.getClass().getMethod("getPointers");
            Object value = getPointers.invoke(touchStateModel);
            if (!(value instanceof Map<?, ?> pointers)) return;

            Set<Integer> seenPointers = new HashSet<>();
            for (Map.Entry<?, ?> entry : pointers.entrySet()) {
                if (!(entry.getKey() instanceof Integer pointerId)) continue;
                Object pointer = entry.getValue();
                if (pointer == null) continue;

                Object state = pointer.getClass().getMethod("getState").invoke(pointer);
                if (state != null && state.getClass().getSimpleName().equals("Released")) {
                    ACTIVE_TOUCH_POINTERS.remove(pointerId);
                    continue;
                }

                seenPointers.add(pointerId);
                if (!ACTIVE_TOUCH_POINTERS.add(pointerId)) continue;

                long packedPosition = readPackedPosition(pointer);
                double normalizedX = Float.intBitsToFloat((int) (packedPosition >>> 32));
                double normalizedY = Float.intBitsToFloat((int) packedPosition);
                double screenX = normalizedX * client.getWindow().getScaledWidth();
                double screenY = normalizedY * client.getWindow().getScaledHeight();
                handleTap(client, screenX, screenY);
                if (isInsideTouchProtectedArea(screenX, screenY)) {
                    suppressTouchControllerView(pointer);
                }
            }
            ACTIVE_TOUCH_POINTERS.retainAll(seenPointers);
        } catch (ClassNotFoundException ignored) {
            // TouchController is optional. Mouse input remains available.
        } catch (Throwable ignored) {
            // Avoid breaking the HUD if TouchController changes its internal API.
        }
    }

    private static boolean isInsideTouchProtectedArea(double x, double y) {
        return x >= X - 4 && x <= X + TOTAL + 4
            && y >= Y - 4 && y <= BUTTON_Y + BUTTON_HEIGHT;
    }

    private static void suppressTouchControllerView(Object pointer) throws ReflectiveOperationException {
        if (pointerStateField == null) {
            pointerStateField = pointer.getClass().getDeclaredField("state");
            pointerStateField.setAccessible(true);
        }
        if (invalidPointerState == null) {
            Class<?> invalidClass = Class.forName(
                "top.fifthlight.touchcontroller.common.state.PointerState$Invalid"
            );
            invalidPointerState = invalidClass.getField("INSTANCE").get(null);
        }
        pointerStateField.set(pointer, invalidPointerState);
    }

    private static long readPackedPosition(Object pointer) throws ReflectiveOperationException {
        for (Method method : pointer.getClass().getDeclaredMethods()) {
            if (method.getName().startsWith("getPosition")) {
                method.setAccessible(true);
                Object result = method.invoke(pointer);
                if (result instanceof Number number) return number.longValue();
            }
        }
        Field field = pointer.getClass().getDeclaredField("position");
        field.setAccessible(true);
        return ((Number) field.get(pointer)).longValue();
    }

    private static void handleTap(MinecraftClient client, double x, double y) {
        if (client.player == null) return;

        if (client.currentScreen instanceof InventoryScreen) {
            if (x >= X && x <= X + TOTAL && y >= BUTTON_Y && y <= BUTTON_Y + BUTTON_HEIGHT) {
                client.setScreen(null);
            }
            return;
        }
        if (client.currentScreen != null) return;

        if (x >= X && x <= X + TOTAL && y >= BUTTON_Y && y <= BUTTON_Y + BUTTON_HEIGHT) {
            client.setScreen(new InventoryScreen(client.player));
            return;
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = X + col * (SIZE + GAP);
                int slotY = Y + row * (SIZE + GAP);
                if (x >= slotX && x < slotX + SIZE && y >= slotY && y < slotY + SIZE) {
                    client.player.getInventory().setSelectedSlot(row * 3 + col);
                    return;
                }
            }
        }
    }

    private static void renderHud(MinecraftClient client, DrawContext graphics) {
        if (client.player == null) return;
        if (client.currentScreen == null) handleTouchControllerPointers(client);
        if (client.currentScreen == null) drawHotbarPanel(client, graphics, false);
    }

    private static void drawHotbarPanel(MinecraftClient client, DrawContext graphics, boolean inventoryOpen) {
        graphics.fill(X - 4, Y - 4, X + TOTAL + 4, Y + TOTAL + 4, 0xAA151515);
        int selectedSlot = client.player.getInventory().getSelectedSlot();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = row * 3 + col;
                int slotX = X + col * (SIZE + GAP);
                int slotY = Y + row * (SIZE + GAP);
                int borderColor = slot == selectedSlot ? 0xFFFFC857 : 0xFF555555;
                graphics.fill(slotX, slotY, slotX + SIZE, slotY + SIZE, borderColor);
                graphics.fill(slotX + 2, slotY + 2, slotX + SIZE - 2, slotY + SIZE - 2, 0xFF252525);

                ItemStack stack = client.player.getInventory().getStack(slot);
                if (!stack.isEmpty()) {
                    int itemX = slotX + (SIZE - 16) / 2;
                    int itemY = slotY + (SIZE - 16) / 2;
                    graphics.drawItem(stack, itemX, itemY);
                    graphics.drawStackOverlay(client.textRenderer, stack, itemX, itemY);
                }
            }
        }

        if (!inventoryOpen) {
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
}
