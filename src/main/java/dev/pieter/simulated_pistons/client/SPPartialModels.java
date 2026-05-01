package dev.pieter.simulated_pistons.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.pieter.simulated_pistons.SimulatedPistons;

public class SPPartialModels {
    public static final PartialModel SHAFT_SIXTEENTH = block("simulated_piston/partials/shaft_sixteenth");
    public static final PartialModel PISTON_SHAFT_SEGMENT = block("simulated_piston/partials/piston_shaft_segment");
    public static final PartialModel CONTROLLER_GEAR = block("simulated_piston/partials/controller_gear");

    private static PartialModel block(final String path) {
        return PartialModel.of(SimulatedPistons.path("block/" + path));
    }

    public static void init() {
        // Initializes static partial model registrations before model baking.
    }
}
