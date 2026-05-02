# Blockbench Model Editing

The piston models are self-contained under the `simulated_pistons` asset namespace, but some current placeholder textures and model parts still need provenance review before public redistribution. See [license-audit.md](license-audit.md) before packaging release files.

Open these files in Blockbench:

- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/controller.json`
- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/single.json`
- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/head.json`
- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/head_unassembled.json`
- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/item.json`
- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/parts/piston_link_plate.json`
- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/parts/piston_shaft_axis_y.json`

Placeholder textures currently live here:

- `src/main/resources/assets/simulated_pistons/textures/block/simulated_piston/piston_frame.png`
- `src/main/resources/assets/simulated_pistons/textures/block/simulated_piston/piston_shaft.png`

Do not treat those texture files as cleared final art. Replace them with original or explicitly permissioned assets before public distribution unless the license audit is updated with confirmed permission.

The blockstate files rotate these models for each facing direction, so author the models in the default upward orientation.
