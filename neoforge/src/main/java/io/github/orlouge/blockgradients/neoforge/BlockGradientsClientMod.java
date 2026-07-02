package io.github.orlouge.blockgradients.neoforge;

import io.github.orlouge.blockgradients.BlockMapScreen;
import io.github.orlouge.blockgradients.Config;
import io.github.orlouge.blockgradients.Constants;
import io.github.orlouge.blockgradients.KeyBindings;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = Constants.MOD_ID)
public class BlockGradientsClientMod {
    public static void init(IEventBus eventBus) {
        Config.loadConfig();
        eventBus.addListener(BlockGradientsClientMod::registerBindings);
    }

    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_BLOCKMAP_KEY_BINDING.get());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (KeyBindings.OPEN_BLOCKMAP_KEY_BINDING.get().consumeClick() && minecraft.player != null && minecraft.level != null) {
            BlockMapScreen.openBlockMap(minecraft);
        }
    }
}
