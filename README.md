# Create: Simulated Pistons

Create: Simulated Pistons is an experimental Create Aeronautics addon for stackable, kinetic pistons. The design target is a Simulated/Aeronautics actuator, closer to the Simulated swivel bearing than to Create's mechanical piston.

## Concept

The mod adds a `Simulated Piston` block. One block provides one block of extension. Placing more simulated pistons in a straight line extends the possible stroke by the number of aligned piston blocks.

An aligned piston chain is classified automatically:

- `Single`: a one-block piston with both the drive cog side and the contraption connection side.
- `Controller`: the first block in a multi-block chain. This is the kinetic input side.
- `Middle`: a housing segment that continues the piston body.
- `Head`: the final segment. This is the intended contraption attachment side.

Every segment exposes a shaft along the piston axis, so rotational power can pass through the piston chain like an extendable shaft. Extension is actuated separately by the integrated side cog, matching the Simulated swivel bearing pattern. The maximum stroke is the number of connected piston segments.

## Design Boundary

This mod should not recreate Create's mechanical piston behavior. The piston should not use extension poles, should not place the moved structure when stopped, and should not rely on Create's piston contraption lifecycle as the final model.

The intended behavior is:

- Build piston length from aligned piston blocks, not separate pole blocks.
- Use the center shaft as rotational passthrough for attached machinery.
- Use the side cog as the actuator input.
- Assemble and move an Aeronautics/Simulated-compatible sublevel contraption, like the swivel bearing does.
- Use empty-hand right click as a reset/return action, following the swivel bearing interaction style.
- Keep the attached contraption live while moving or stopped, rather than placing it into the world like a Create mechanical piston.

## Current Prototype

This first implementation is a basic working concept:

- Registers a NeoForge addon mod for Minecraft 1.21.1.
- Adds a placeable `simulated_piston` block and item.
- Automatically connects aligned piston blocks and updates their controller/middle/head/single texture state.
- Provides Create kinetic shaft connectivity along the piston axis for passthrough power.
- Adds a Simulated-style extra cog kinetic input for piston actuation.
- Tracks extension progress from the side cog speed in the kinetic block entity for testing.
- Empty-hand right click assembles the blocks in front of the piston head into a Simulated sublevel, or disassembles/resets if already assembled.
- Includes placeholder block models, blockstates, lang, loot table, recipe, and tags.

The prototype can assemble/disassemble an attached Simulated sublevel, but it does not yet constrain or move that sublevel linearly from `Extension`. That is the next major step.

## Reference

This repository was scaffolded against the local Aeronautics/Simulated source tree at:

```text
D:\Git\Simulated-Project
```

Important reference points:

- `simulated/common/.../swivel_bearing`: kinetic controller, cogwheel behavior, plate/link concept, and rotational passthrough.
- `aeronautics/common/.../propeller/bearing`: Aeronautics bearing integration and controlled contraption entity patterns.
- `simulated/common/.../SimAssemblyContraption`: special assembly treatment for Simulated bearing blocks.

## Development

Build:

```powershell
.\gradlew build
```

Run a client:

```powershell
.\gradlew runClient
```

In-game, craft or give yourself:

```text
/give @p simulated_pistons:simulated_piston
```

Place several pistons in a line facing the same direction. Wrench rotation changes the facing. Connect Create rotational power along the piston axis to inspect kinetic passthrough. Place and power a cogwheel beside the controller to actuate extension progress. Put a test structure in front of the piston head and empty-hand right click the piston to assemble it into a Simulated sublevel; empty-hand right click again to disassemble/reset.

Useful debug command:

```text
/data get block <x> <y> <z>
```

Important fields:

- `Extension`: current tracked piston extension.
- `ChainLength`: maximum extension from aligned piston blocks.
- `LastActuatorSpeed`: side-cog input seen by the piston.
- `SubLevelID`: present when an attached sublevel is assembled.
- `SubLevelAnchor` and `DisassemblyGoal`: stored mapping used for returning the sublevel.
- `LastAssemblyStatus`: last assembly/disassembly result.

## Investigation Notes: Sublevel Piston Head Assembly

Bug under investigation: when a simulated piston is itself inside a Sable sublevel, the piston head/link is only misplaced during empty assembly. Assembly with blocks attached works correctly, so the main suspect is the discrepancy between the normal payload path and the custom empty-sublevel workaround.

Known-good path to align with:

- [`assembleAttachment`](src/main/java/dev/pieter/simulated_pistons/content/SimulatedPistonBlockEntity.java#L295) computes `headPos` and `toAssemble` once, then branches between payload and empty assembly.
- [`assemblePayload`](src/main/java/dev/pieter/simulated_pistons/content/SimulatedPistonBlockEntity.java#L340) delegates to `SimAssemblyHelper.assembleFromSingleBlock`, and this path works when blocks are attached. Treat the returned `AssemblyResult` as the reference contract: an assembled sublevel plus a plot offset.
- [`assembleAttachment`](src/main/java/dev/pieter/simulated_pistons/content/SimulatedPistonBlockEntity.java#L315) through [`assembleAttachment`](src/main/java/dev/pieter/simulated_pistons/content/SimulatedPistonBlockEntity.java#L330) performs the final bookkeeping for a payload result: save sublevel id, compute `SubLevelAnchor`, `DisassemblyGoal`, and `linkPos`, place the link, mark the head assembled, capture the base pose, attach the constraint, and sync status.

Empty path discrepancy suspects:

- [`assembleAttachment`](src/main/java/dev/pieter/simulated_pistons/content/SimulatedPistonBlockEntity.java#L301) skips `assemblePayload` entirely when `emptyFront` is true, so the empty case never receives the same `AssemblyResult` shape as the payload path.
- [`assembleEmpty`](src/main/java/dev/pieter/simulated_pistons/content/SimulatedPistonBlockEntity.java#L353) manually allocates a new sublevel, places the link in the embedded level, derives a pose, computes an offset, kicks from the containing sublevel, teleports, and then does its own subset of the payload bookkeeping.
- [`assembleEmpty`](src/main/java/dev/pieter/simulated_pistons/content/SimulatedPistonBlockEntity.java#L359) and [`assembleEmpty`](src/main/java/dev/pieter/simulated_pistons/content/SimulatedPistonBlockEntity.java#L374) are likely where the wrong spawn position enters: this path invents the sublevel pose from `headPos` instead of using the same assembly helper/result semantics as the working payload path.
- [`assembleEmpty`](src/main/java/dev/pieter/simulated_pistons/content/SimulatedPistonBlockEntity.java#L382) through [`assembleEmpty`](src/main/java/dev/pieter/simulated_pistons/content/SimulatedPistonBlockEntity.java#L389) duplicates the offset/bookkeeping logic from the payload path. This should probably become a shared finalization method so the two paths cannot drift.
- [`attachPistonConstraint`](src/main/java/dev/pieter/simulated_pistons/content/SimulatedPistonBlockEntity.java#L535) may still need checking, but because payload assembly works, constraint coordinates are less likely to be the root cause than the empty sublevel creation/offset path.

Reference behavior in the local Simulated source tree:

- `D:\Git\Simulated-Project\simulated\common\src\main\java\dev\simulated_team\simulated\util\SimAssemblyHelper.java:136` is the working payload assembly helper. A useful experiment is to see whether the empty/link-only case can be represented as a tiny block assembly instead of manually allocating a sublevel.
- `D:\Git\Simulated-Project\simulated\common\src\main\java\dev\simulated_team\simulated\content\blocks\swivel_bearing\SwivelBearingBlockEntity.java:400` through `:480` is the closest reference for creating/placing the bearing link block. It has the same empty-sublevel pattern as this piston, but the piston bug shows this workaround is fragile when the controller is already inside a sublevel.
- `D:\Git\Simulated-Project\simulated\common\src\main\java\dev\simulated_team\simulated\content\blocks\physics_assembler\PhysicsAssemblerBlockEntity.java:193` computes a disassembly goal with `subLevel.logicalPose().transformPosition(Vec3.atCenterOf(this.getBlockPos()))`; keep this in mind if empty assembly still needs explicit projection after being aligned with the payload path.

Useful debug checks:

- Add temporary debug data for both branches: `headPos`, `toAssemble`, returned/manual `offset`, `linkPos`, `SubLevelAnchor`, `DisassemblyGoal`, sublevel logical pose, and containing sublevel id.
- Test three cases side by side: payload assembly in real level, empty assembly in real level, and empty assembly from a piston mounted to a moved/rotated sublevel. The first case is the baseline; the second catches empty-path-only regressions; the third should reproduce this bug.
- Refactor direction to try first: create a shared finalization helper that accepts `(SubLevel subLevel, BlockPos offset, Direction facing, BlockPos headPos, BlockPos toAssemble, boolean hasPayload)` and use it for both payload and empty results.
- Refactor direction to investigate next: remove the manual empty workaround by creating/placing the link as a normal temporary block and letting `SimAssemblyHelper.assembleFromSingleBlock` produce the same `AssemblyResult` as the payload path, then finalize through the shared helper.

Build note: this workspace builds with Java 21 from `C:\Program Files\JetBrains\WebStorm 2025.3.3\jbr` and a repo-local Gradle home:

```powershell
$env:JAVA_HOME='C:\Program Files\JetBrains\WebStorm 2025.3.3\jbr'
$env:GRADLE_USER_HOME='D:\Git\Simulated-Pistons\.gradle-home'
.\gradlew.bat build
```

## Roadmap

- Add a proper piston head/link block inside the assembled sublevel, equivalent to the swivel bearing plate.
- Add a linear physics constraint or pose controller so the assembled sublevel follows cog-driven `Extension`.
- Keep the sublevel assembled while stopped; do not place it back into the world as normal Create piston behavior.
- Preserve center-shaft rotational passthrough into the attached sublevel while extended.
- Add visible moving shaft/head rendering and connected housing/cog/head art.
