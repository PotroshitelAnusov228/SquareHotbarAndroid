package dev.squarehotbar;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SquareHotbarClient implements ClientModInitializer {
    public static final String MOD_ID = "squarehotbar";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("SquareHotbar initialized!");
    }
}
