package io.github.orlouge.blockgradients.forge;

import io.github.orlouge.blockgradients.BlockMapScreen;
import io.github.orlouge.blockgradients.Config;
import io.github.orlouge.blockgradients.KeyBindings;
import net.minecraft.client.MinecraftClient;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class BlockGradientsClientMod {
    public static int init() {
        Config.loadConfig();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(BlockGradientsClientMod::registerBindings);
        MinecraftForge.EVENT_BUS.addListener(BlockGradientsClientMod::onClientTick);
        return 0;
    }

    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_BLOCKMAP_KEY_BINDING.get());
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (KeyBindings.OPEN_BLOCKMAP_KEY_BINDING.get().wasPressed() && mc.player != null && mc.player.getWorld() != null) {
                BlockMapScreen.openBlockMap(mc);
            }
        }
    }
}
