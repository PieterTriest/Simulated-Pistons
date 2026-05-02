# Development Notes

## Build

Use Java 21.

```powershell
.\gradlew build
```

Run a client:

```powershell
.\gradlew runClient
```

In-game test item:

```text
/give @p simulated_pistons:simulated_piston
```

## Prototype Scope

This mod intentionally does not recreate Create's mechanical piston lifecycle. The intended behavior is:

- Build piston length from aligned piston blocks, not separate pole blocks.
- Use the center shaft as rotational passthrough for attached machinery.
- Use the side cog as actuator input.
- Assemble and move a Simulated/Aeronautics-compatible sublevel contraption.
- Use empty-hand right click as a reset/return action, following the Simulated swivel bearing interaction style.
- Keep the attached contraption live while moving or stopped instead of placing it back into the world like a Create mechanical piston.

## Useful Data Checks

The block entity stores these fields for diagnostics:

- `Extension`: current tracked piston extension.
- `ChainLength`: maximum extension from aligned piston blocks.
- `LastActuatorSpeed`: side-cog input seen by the piston.
- `LastMovementSpeed`: computed movement speed from the actuator.
- `LastTargetExtension`: target side implied by the current motion direction.
- `SubLevelID`: present when an attached sublevel is assembled.
- `SubLevelAnchor` and `DisassemblyGoal`: stored mapping used for returning the sublevel.
- `LastAssemblyStatus`: last assembly/disassembly result.
- `LastMotionStatus`: last movement or constraint status.

In-game command:

```text
/data get block <x> <y> <z>
```

## Investigation Index

- [Empty-head assembly inside Sable sublevels](investigations/sublevel-head-assembly.md)
- [Blockbench model editing](BLOCKBENCH_MODELS.md)

## License Hygiene

Before publishing a build, review [license-audit.md](license-audit.md). Upstream Create and Simulated/Aeronautics code is MIT, but upstream assets are not freely redistributable under MIT. Install testing should use the official Create, Create Aeronautics, and Sable packages; bundled modules such as Create Simulated, Create Offroad, Sable Companion, Flywheel, and Ponder may appear in the mod list.
