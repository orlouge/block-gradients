package io.github.orlouge.blockgradients.neoforge;

import io.github.orlouge.blockgradients.Constants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.bus.api.IEventBus;

@Mod(Constants.MOD_ID)
public class BlockGradientsMod {
    public BlockGradientsMod(IEventBus eventBus, ModContainer container) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            BlockGradientsClientMod.init(eventBus);
        }
    }
}
