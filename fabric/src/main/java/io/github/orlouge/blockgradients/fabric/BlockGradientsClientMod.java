package io.github.orlouge.blockgradients.fabric;

import io.github.orlouge.blockgradients.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class BlockGradientsClientMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("blockmap");

    @Override
    public void onInitializeClient() {
        Config.loadConfig();

        KeyBindingHelper.registerKeyBinding(KeyBindings.OPEN_BLOCKMAP_KEY_BINDING.get());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (KeyBindings.OPEN_BLOCKMAP_KEY_BINDING.get().wasPressed() && mc.player != null && mc.player.getWorld() != null) {
                BlockMapScreen.openBlockMap(mc);
            }
        });
    }
}
