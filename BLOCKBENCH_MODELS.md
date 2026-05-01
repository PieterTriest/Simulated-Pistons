# Blockbench Model Editing

The piston models are now self-contained under the `simulated_pistons` asset namespace.

Open these files in Blockbench:

- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/controller.json`
- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/single.json`
- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/head.json`
- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/head_unassembled.json`
- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/parts/bearing_plate.json`
- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/parts/auger_axis_y.json`

Local texture copies live here:

- `src/main/resources/assets/simulated_pistons/textures/block/simulated_piston/swivel_bearing.png`
- `src/main/resources/assets/simulated_pistons/textures/block/simulated_piston/auger.png`

The blockstate files still rotate these models for each facing direction, so author the models in the default upward orientation.
