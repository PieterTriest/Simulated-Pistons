package dev.pieter.simulated_pistons.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.pieter.simulated_pistons.SimulatedPistons;

public class SPPartialModels {
    public static final PartialModel SHAFT_SIXTEENTH = block("simulated_piston/partials/shaft_sixteenth");

    private static PartialModel block(final String path) {
        return PartialModel.of(SimulatedPistons.path("block/" + path));
    }

    public static void init() {
        // Initializes static partial model registrations before model baking.
    }
}
