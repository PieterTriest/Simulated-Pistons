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

## Roadmap

- Add a proper piston head/link block inside the assembled sublevel, equivalent to the swivel bearing plate.
- Add a linear physics constraint or pose controller so the assembled sublevel follows cog-driven `Extension`.
- Keep the sublevel assembled while stopped; do not place it back into the world as normal Create piston behavior.
- Preserve center-shaft rotational passthrough into the attached sublevel while extended.
- Add visible moving shaft/head rendering and connected housing/cog/head art.
