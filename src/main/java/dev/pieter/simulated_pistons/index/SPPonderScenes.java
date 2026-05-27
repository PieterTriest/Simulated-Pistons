package dev.pieter.simulated_pistons.index;

import dev.pieter.simulated_pistons.ponder.scenes.SimulatedPistonScenes;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SPPonderScenes {
    public static void register(final PonderSceneRegistrationHelper<ResourceLocation> registry) {
        final PonderSceneRegistrationHelper<DeferredHolder<?, ?>> helper = registry.withKeyFunction(DeferredHolder::getId);

        helper.forComponents(SPBlocks.SIMULATED_PISTON)
                .addStoryBoard("simulated_pistons/simulated_piston_ponder_scene_1", SimulatedPistonScenes::intro);
    }
}
