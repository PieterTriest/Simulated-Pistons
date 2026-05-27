package dev.pieter.simulated_pistons;

import dev.pieter.simulated_pistons.client.SPPartialModels;
import dev.pieter.simulated_pistons.ponder.SPPonderPlugin;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = SimulatedPistons.MOD_ID, dist = Dist.CLIENT)
public class SimulatedPistonsClient {
    public SimulatedPistonsClient() {
        SPPartialModels.init();
        PonderIndex.addPlugin(new SPPonderPlugin());
    }
}
