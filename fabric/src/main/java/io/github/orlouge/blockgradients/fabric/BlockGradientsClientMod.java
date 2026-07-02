package io.github.orlouge.blockgradients.fabric;

import io.github.orlouge.blockgradients.BlockMapScreen;
import io.github.orlouge.blockgradients.Config;
import io.github.orlouge.blockgradients.KeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;

public class BlockGradientsClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.loadConfig();
        KeyMappingHelper.registerKeyMapping(KeyBindings.OPEN_BLOCKMAP_KEY_BINDING.get());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (KeyBindings.OPEN_BLOCKMAP_KEY_BINDING.get().consumeClick() && minecraft.player != null && minecraft.level != null) {
                BlockMapScreen.openBlockMap(minecraft);
            }
        });
    }
}
