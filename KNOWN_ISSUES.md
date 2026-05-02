# Known Issues

This project is in public-alpha preparation. Back up worlds before testing.

## Current Issues

- Linear movement is not implemented as finished gameplay yet. Extension is tracked, but the piston should not be treated as a production-ready moving actuator.
- Models and textures are placeholders or work in progress.
- Some placeholder asset provenance is not resolved for public redistribution yet. See `docs/license-audit.md`.
- Empty-head assembly inside an existing Sable sublevel has a known placement bug. Assembly with a payload in front of the head is the better-tested path.
- Sublevel physics constraints are experimental and may fail to restore after world reloads or unusual sublevel state changes.
- Dedicated server and multiplayer behavior need more testing.
- Recipes, balancing, tooltips, and in-game documentation are minimal.

Use throwaway worlds or current backups while testing alpha builds.
