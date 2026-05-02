# Empty-Head Assembly Inside Sable Sublevels

## Summary

When a Simulated Piston is itself inside a Sable sublevel, the piston head/link can be misplaced during empty-head assembly. Assembly with blocks attached works better, so the current suspicion is a mismatch between the normal payload assembly path and the custom empty-sublevel workaround.

## Known-Good Path

- `assembleAttachment` computes `headPos` and `toAssemble` once, then branches between payload and empty assembly.
- `assemblePayload` delegates to `SimAssemblyHelper.assembleFromSingleBlock`; this path works when blocks are attached.
- The returned assembly result provides an assembled sublevel plus a plot offset.
- Payload finalization saves the sublevel id, computes `SubLevelAnchor`, `DisassemblyGoal`, and `linkPos`, places the link, marks the head assembled, captures the base pose, attaches the constraint, and syncs status.

## Empty Path Discrepancy Suspects

- The empty case skips `assemblePayload`, so it never receives the same assembly result shape as the payload path.
- The empty path manually allocates a new sublevel, places the link in the embedded level, derives a pose, computes an offset, teleports, and performs its own subset of payload bookkeeping.
- The wrong spawn position likely enters where the empty path invents the sublevel pose from `headPos`.
- Offset and bookkeeping logic is duplicated between payload and empty assembly and should be shared if this path remains.
- Constraint coordinates may still need checking, but payload assembly working makes empty sublevel creation/offset more suspicious.

## Upstream Reference Concepts

- Create Simulated's `SimAssemblyHelper.assembleFromSingleBlock` is the reference payload assembly helper.
- Create Simulated's swivel bearing block entity has a similar empty-sublevel pattern for bearing links.
- Create Simulated's physics assembler projects disassembly goals through the sublevel logical pose; this may matter if empty assembly still needs explicit projection.

## Tests To Run

- Payload assembly in the real level.
- Empty assembly in the real level.
- Empty assembly from a piston mounted to a moved or rotated Sable sublevel.

## Refactor Direction

First, create a shared finalization helper for payload and empty results. It should accept the assembled sublevel, offset, facing, head position, assembly target, and payload flag, then perform all common bookkeeping in one place.

Next, investigate whether the empty/link-only case can be represented as a tiny normal assembly so `SimAssemblyHelper.assembleFromSingleBlock` produces the same result contract as the payload path.
