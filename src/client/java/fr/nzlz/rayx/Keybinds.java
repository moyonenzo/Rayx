package fr.nzlz.rayx;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public final class Keybinds {
    private static KeyMapping toggleKey;

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(
                    Rayx.id("main")
            );

    private Keybinds() {
    }

    public static void initialize() {
        toggleKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.rayx.toggle",
                        InputConstants.KEY_X,
                        CATEGORY
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                States.toggle();
            }
        });
    }
}