# Create: Simulated Pistons

Create: Simulated Pistons is a stackable piston system for Create Aeronautics that moves live sublevel contraptions with kinetic passthrough. The goal is a Create-style actuator for Sable sublevel contraptions: piston bodies define the stroke, the center shaft carries rotation, and a side cog drives extension.

## Alpha Warning

This project is not release-stable. It is being prepared for public alpha testing and can still break contraptions, save data, or worlds. Back up your world before testing, especially when using the piston inside Sable sublevels or with valuable contraptions attached.

Linear piston movement is still incomplete. Treat the current build as a prototype for assembly, block state behavior, kinetic passthrough, and early sublevel integration.

## Requirements

Tested with:

- Minecraft `1.21.1`
- NeoForge `21.1.228`
- Create `6.0.10`
- Create Aeronautics `1.2.1`
- Sable `1.2.2`

Create Aeronautics and Sable may load additional bundled modules such as Create Simulated, Create Offroad, Sable Companion, Vail, Flywheel, and Ponder. These can appear in the in-game mod list, but users should generally install the official Create, Create Aeronautics, and Sable packages rather than searching for every internal module separately.

The mod is required on both client and server.

## Basic Usage

1. Install the required dependencies.
2. Place one or more Simulated Piston blocks in a straight line facing the same direction.
3. Use a wrench to rotate the piston facing.
4. Connect Create rotational power along the piston axis for shaft passthrough.
5. Place and power a cogwheel beside the controller segment to drive the actuator input.
6. Empty-hand right click the controller to assemble the blocks in front of the piston head into a Sable sublevel.
7. Empty-hand right click again to disassemble or reset the attachment.

Each piston segment contributes one block of maximum stroke. A single piston acts as both controller and head; a longer line is classified as controller, middle segments, and head.

## Implemented Behavior

- Registers a NeoForge addon mod for Minecraft 1.21.1.
- Adds the `simulated_pistons:simulated_piston` block and item.
- Automatically classifies aligned piston chains as single, controller, middle, or head.
- Provides Create kinetic shaft connectivity along the piston axis.
- Adds a side cog kinetic input for piston actuation.
- Tracks extension progress from actuator speed.
- Assembles and disassembles a Sable sublevel attachment from the piston head.
- Stores assembly and motion status in block entity data for diagnostics.
- Includes early block models, blockstates, language data, loot table, recipe, and tags.

## Known Limitations

- Linear movement is not implemented as finished gameplay yet.
- Physics constraints and sublevel placement are experimental.
- Models and textures are placeholders or work in progress.
- Empty-head assembly from a piston already inside an existing Sable sublevel has a known placement bug.
- There is no polished in-game documentation or Ponder scene yet.
- Compatibility beyond the versions listed above is not tested.

See [KNOWN_ISSUES.md](KNOWN_ISSUES.md) for the current public issue list.

## Roadmap

- Finish reliable linear movement for assembled sublevels.
- Stabilize empty-head assembly inside existing Sable sublevels.
- Preserve center-shaft rotational passthrough into attached sublevels while extended.
- Replace placeholder models and textures with redistributable final assets.
- Add proper gameplay balancing, recipes, tooltips, and in-game documentation.
- Broaden testing across dedicated servers and common Create/Aeronautics setups.

## Reporting Issues

When reporting issues, include:

- Minecraft, NeoForge, Create, Create Aeronautics, Sable, and Create: Simulated Pistons versions.
- Any bundled module versions shown in the mod list if they look relevant, especially Create Simulated, Sable Companion, Flywheel, or Ponder.
- Whether the issue happens in singleplayer, LAN, or dedicated server.
- A short reproduction with piston chain length, facing, attached blocks, and whether the piston is inside a Sable sublevel.
- Relevant logs or crash reports.
- Screenshots or a small test world when placement or movement is wrong.

Please test on a backed-up world and mention whether other mods are installed.

## Credits

Inspired by Create, Create Aeronautics, Create Simulated, and Sable. The piston design borrows concepts from Create kinetic blocks and Create Simulated/Create Aeronautics sublevel controllers, especially the swivel bearing style of interaction and assembly.

## License

Original code and documentation in this repository are licensed under MIT. Adapted Create Simulated API stubs require upstream MIT notice, and unresolved copied or derived assets are not cleared for public redistribution. Do not assume the repository as a whole is ready to redistribute until the audit blockers are resolved. See [docs/license-audit.md](docs/license-audit.md) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
