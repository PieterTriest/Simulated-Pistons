package dev.pieter.simulated_pistons.ponder;

import com.simibubi.create.foundation.ponder.CreatePonderPlugin;
import dev.pieter.simulated_pistons.SimulatedPistons;
import dev.pieter.simulated_pistons.index.SPPonderScenes;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class SPPonderPlugin extends CreatePonderPlugin {
    @Override
    public String getModId() {
        return SimulatedPistons.MOD_ID;
    }

    @Override
    public void registerScenes(final PonderSceneRegistrationHelper<ResourceLocation> helper) {
        SPPonderScenes.register(helper);
    }
}
