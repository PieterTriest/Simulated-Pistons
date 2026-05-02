# Blockbench Model Editing

The piston models are self-contained under the `simulated_pistons` asset namespace, but some current placeholder textures and model parts still need provenance review before public redistribution. See [license-audit.md](license-audit.md) before packaging release files.

Open these files in Blockbench:

- `src/generated/resources/assets/simulated_pistons/models/block/simulated_piston/controller.json`
- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/single.json`
- `src/generated/resources/assets/simulated_pistons/models/block/simulated_piston/middle.json`
- `src/generated/resources/assets/simulated_pistons/models/block/simulated_piston/head.json`
- `src/generated/resources/assets/simulated_pistons/models/block/simulated_piston/head_unassembled.json`
- `src/generated/resources/assets/simulated_pistons/models/block/simulated_piston/item.json`
- `src/generated/resources/assets/simulated_pistons/models/block/simulated_piston/parts/piston_link_plate.json`
- `src/generated/resources/assets/simulated_pistons/models/block/simulated_piston/partials/controller_gear.json`
- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/partials/shaft_sixteenth.json`
- `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/partials/piston_shaft_segment.json`

## Model Ownership

Use these names when discussing or editing piston geometry. The most important distinction is that the terminal piston head and the piston link plate are different parts.

| Concept | Current file | Purpose | Notes |
| --- | --- | --- | --- |
| `controller_base` | generated `controller.json`, generated `item.json`, generated `single_unassembled.json`, hand-authored `single.json` | Stationary controller housing/base geometry. | Edit `source_parts/controller_base.json` for generated models. |
| `controller_short_case` | generated `controller.json`, generated `item.json`, generated `single_unassembled.json`, hand-authored `single.json` | Short piston case attached to the controller/single body. | Edit `source_parts/controller_short_case.json` for generated models. |
| `controller_gear` | generated `partials/controller_gear.json` | Animated gear rendered by Java. | Edit `source_parts/controller_gear.json`. Do not bake it into in-world controller/single-unassembled block models. |
| `middle_case` | generated `middle.json` | Full-length middle piston case for multi-block pistons. | Edit `source_parts/middle_shaft.json`. |
| `terminal_head` | generated `head.json` | Last piston block in a piston chain. | Edit `source_parts/terminal_head.json`. This is not the link plate. |
| `head_shaft_stub` | generated `head_unassembled.json` | Shaft/body part of the disassembled terminal head. | Edit `source_parts/head_shaft_stub.json`. |
| `piston_link_plate` | generated `parts/piston_link_plate.json`, embedded in generated composites | Reusable attachment/link plate used by the standalone piston link block and disassembled piston composites. | Edit `source_parts/piston_link_plate.json`. |
| `shaft_end` | `partials/shaft_sixteenth.json` | Small rotating shaft end rendered by Java. | Runtime partial; referenced from `SPPartialModels`. |
| `shaft_segment` | `partials/piston_shaft_segment.json` | Repeated rotating shaft segment rendered by Java for piston links. | Runtime partial; referenced from `SPPartialModels`. |
| `item_composite` | `item.json` | Inventory/item presentation. | Planned generated model matching `single_unassembled.json`. |

## Planned Generated Models

Editable source parts live under:

`src/main/resources/assets/simulated_pistons/models/block/simulated_piston/source_parts/`

The generation manifest lives at:

`src/main/resources/assets/simulated_pistons/models/block/simulated_piston/model_parts.json`

Generated game-facing models should be emitted under:

`src/generated/resources/assets/simulated_pistons/models/block/simulated_piston/`

Generated models currently include:

- `controller.json`
- `middle.json`
- `head.json`
- `single_unassembled.json`
- `item.json`
- `head_unassembled.json`
- `parts/piston_link_plate.json`
- `partials/controller_gear.json`

`controller.json` is composed from:

- `source_parts/controller_base.json`
- `source_parts/controller_short_case.json`

Do not include `source_parts/piston_link_plate.json` or `source_parts/controller_gear.json` in `controller.json`: multi-piston controller segments do not have an attachment face, and the block entity renderer draws the controller gear dynamically.

`single_unassembled.json` is composed from:

- `source_parts/controller_base.json`
- `source_parts/controller_short_case.json`
- `source_parts/piston_link_plate.json`

Do not include `source_parts/controller_gear.json` in `single_unassembled.json`: the block entity renderer draws the controller gear dynamically for `SINGLE` and `CONTROLLER` segments.

`item.json` is composed from:

- `source_parts/controller_base.json`
- `source_parts/controller_gear.json`
- `source_parts/controller_short_case.json`
- `source_parts/piston_link_plate.json`

`item.json` also preserves display transforms from `source_parts/item_display.json`.

`head_unassembled.json` is composed from:

- `source_parts/head_shaft_stub.json`
- `source_parts/piston_link_plate.json`

`head.json` is composed from:

- `source_parts/terminal_head.json`

`middle.json` is composed from:

- `source_parts/middle_shaft.json`

`parts/piston_link_plate.json` is composed from:

- `source_parts/piston_link_plate.json`

`partials/controller_gear.json` is composed from:

- `source_parts/controller_gear.json`

Generated files should include a `credit` marker saying they are generated from source parts and should not be edited directly.

Run model generation from PowerShell with:

```powershell
$env:JAVA_HOME = "C:\Program Files\JetBrains\WebStorm 2025.3.3\jbr"
$env:GRADLE_USER_HOME = "D:\Git\Simulated-Pistons\.gradle-home"
.\gradlew.bat generatePistonModels
```

Verify committed generated models are current with:

```powershell
$env:JAVA_HOME = "C:\Program Files\JetBrains\WebStorm 2025.3.3\jbr"
$env:GRADLE_USER_HOME = "D:\Git\Simulated-Pistons\.gradle-home"
.\gradlew.bat verifyPistonModels
```

When opening generated models directly from `src/generated/resources` in Blockbench, texture previews may need manual relinking because that generated folder is not next to the texture source root. The JSON should still use namespaced texture references such as `simulated_pistons:block/simulated_piston/piston_frame`, which Minecraft resolves from the resource pack.

Placeholder textures currently live here:

- `src/main/resources/assets/simulated_pistons/textures/block/simulated_piston/piston_frame.png`
- `src/main/resources/assets/simulated_pistons/textures/block/simulated_piston/piston_shaft.png`

Do not treat those texture files as cleared final art. Replace them with original or explicitly permissioned assets before public distribution unless the license audit is updated with confirmed permission.

The blockstate files rotate these models for each facing direction, so author the models in the default upward orientation.
