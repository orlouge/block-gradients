package io.github.orlouge.blockgradients.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("blockgradients")
public class ExampleModForge {
    public ExampleModForge() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> BlockGradientsClientMod::init);
    }
}
