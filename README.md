# Create: Simulated Pistons

Create: Simulated Pistons is an experimental Create Aeronautics addon for stackable, kinetic pistons that behave more like a Simulated swivel bearing than a vanilla Create mechanical piston.

## Concept

The mod adds a `Simulated Piston` block. One block provides one block of extension. Placing more simulated pistons in a straight line extends the possible stroke by the number of aligned piston blocks.

An aligned piston chain is classified automatically:

- `Single`: a one-block piston with both the drive cog side and the contraption connection side.
- `Controller`: the first block in a multi-block chain. This is the kinetic input side.
- `Middle`: a housing segment that continues the piston body.
- `Head`: the final segment. This is the intended contraption attachment side.

Every segment exposes a shaft along the piston axis, so rotational power can pass through the piston chain like an extendable shaft. Extension is actuated separately by the integrated side cog, matching the Simulated swivel bearing pattern: positive cog speed extends, negative cog speed retracts, and the maximum stroke is the number of connected piston segments.

## Current Prototype

This first implementation is a basic working concept:

- Registers a NeoForge addon mod for Minecraft 1.21.1.
- Adds a placeable `simulated_piston` block and item.
- Automatically connects aligned piston blocks and updates their controller/middle/head/single texture state.
- Provides Create kinetic shaft connectivity along the piston axis for passthrough power.
- Adds a Simulated-style extra cog kinetic input for piston actuation.
- Tracks extension progress from the side cog speed in the kinetic block entity for testing.
- Includes placeholder block models, blockstates, lang, loot table, recipe, and tags.

The prototype does not yet move a Create/Aeronautics contraption. That is the next major step: replacing the progress-only block entity with a controlled contraption entity/assembly flow modeled after Simulated's swivel bearing and Create's piston mechanics.

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

Place several pistons in a line facing the same direction. Wrench rotation changes the facing. Connect Create rotational power along the piston axis to inspect kinetic passthrough and extension progress.

## Roadmap

- Add visible moving shaft/head rendering based on extension progress.
- Add a dedicated contraption attachment/link block or head behavior.
- Assemble the attached blocks into a controlled contraption when the controller starts moving.
- Move the contraption linearly instead of only tracking extension progress.
- Preserve kinetic passthrough into the attached contraption while extended.
- Replace placeholder models/textures with connected housing/cog/head art.
