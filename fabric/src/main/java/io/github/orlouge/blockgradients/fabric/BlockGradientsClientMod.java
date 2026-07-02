package io.github.orlouge.blockgradients.fabric;

import io.github.orlouge.blockgradients.BlockMapScreen;
import io.github.orlouge.blockgradients.Config;
import io.github.orlouge.blockgradients.KeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;

public class BlockGradientsClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.loadConfig();
        KeyBindingHelper.registerKeyBinding(KeyBindings.OPEN_BLOCKMAP_KEY_BINDING.get());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (KeyBindings.OPEN_BLOCKMAP_KEY_BINDING.get().consumeClick() && minecraft.player != null && minecraft.level != null) {
                BlockMapScreen.openBlockMap(minecraft);
            }
        });
    }
}
