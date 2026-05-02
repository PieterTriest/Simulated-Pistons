package dev.pieter.simulated_pistons.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.pieter.simulated_pistons.SimulatedPistons;

public class SPPartialModels {
    public static final PartialModel SHAFT_SIXTEENTH = block("simulated_piston/partials/shaft_sixteenth");
    public static final PartialModel PISTON_SHAFT_SEGMENT = block("simulated_piston/partials/piston_shaft_segment");
    public static final PartialModel PISTON_SHAFT_SEGMENT_HEAD = block("simulated_piston/partials/piston_shaft_segment_head");
    public static final PartialModel PISTON_SHAFT_SEGMENT_TAIL = block("simulated_piston/partials/piston_shaft_segment_tail");
    public static final PartialModel PISTON_SHAFT_SEGMENT_SINGLE = block("simulated_piston/partials/piston_shaft_segment_single");
    public static final PartialModel PISTON_SHAFT_SEGMENT_TAIL_EXTENSION = block("simulated_piston/partials/piston_shaft_segment_tail_extension");
    public static final PartialModel CONTROLLER_GEAR = block("simulated_piston/partials/controller_gear");

    private static PartialModel block(final String path) {
        return PartialModel.of(SimulatedPistons.path("block/" + path));
    }

    public static void init() {
        // Initializes static partial model registrations before model baking.
    }
}
