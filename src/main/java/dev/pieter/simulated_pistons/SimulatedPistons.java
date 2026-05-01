package dev.pieter.simulated_pistons;

import dev.pieter.simulated_pistons.index.SPBlockEntityTypes;
import dev.pieter.simulated_pistons.index.SPBlocks;
import dev.pieter.simulated_pistons.content.SPPlacementHelpers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(SimulatedPistons.MOD_ID)
public class SimulatedPistons {
    public static final String MOD_ID = "simulated_pistons";

    public SimulatedPistons(final IEventBus modBus) {
        SPBlocks.register(modBus);
        SPBlockEntityTypes.register(modBus);
        SPPlacementHelpers.init();
    }

    public static ResourceLocation path(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
