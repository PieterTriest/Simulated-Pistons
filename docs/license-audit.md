# License Audit

This is a practical release-prep audit, not legal advice. Anything marked `Needs manual review` should be resolved before a public Modrinth or CurseForge file is published.

## Sources Checked

- This repository: no license file existed before this cleanup; `gradle.properties` already declared `MIT`.
- Create repository license: https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md
- Create Modrinth page: https://modrinth.com/mod/create
- Simulated Project license: https://github.com/Creators-of-Aeronautics/Simulated-Project/blob/main/LICENSE.md
- Create Aeronautics Modrinth page: https://modrinth.com/mod/create-aeronautics
- Local Simulated Project checkout, when available, for file comparison.

## Summary

MIT is suitable for original code and documentation in this repository. It is also compatible with MIT-licensed upstream code from Create and the Create Simulated/Create Aeronautics sources when required notices are preserved.

The blocker is assets: Create and the Create Simulated/Create Aeronautics sources distinguish code from assets, and their asset directories are all-rights-reserved. Any copied or closely adapted upstream texture/model assets should be replaced, permissioned, or excluded before public redistribution. The repository `LICENSE` is intended for original code and documentation; it does not make unresolved third-party assets safe to ship.

## Audit Table

| Area/File | Origin | Copied / Adapted / Inspired / Original | Upstream License | Required Attribution | Action Needed |
|---|---|---|---|---|---|
| `src/main/java/dev/pieter/simulated_pistons/**` | Project implementation with Create, Sable, Create Simulated, and Create Aeronautics API patterns | Original / Inspired | Project MIT for original code; upstream APIs/patterns from MIT code | Credit Create, Sable, Create Simulated, and Create Aeronautics in README | OK for MIT, subject to review for any copied snippets |
| `src/simulatedApiStubs/java/dev/simulated_team/simulated/util/extra_kinetics/ExtraKinetics.java` | Simulated `ExtraKinetics` API shape, reduced local compile stub | Adapted | Simulated Project code license: MIT | Simulated Team / Creators of Aeronautics MIT notice | Notice added in `THIRD_PARTY_NOTICES.md`; replacing with dependency/API-only approach would still be cleaner |
| `src/simulatedApiStubs/java/dev/simulated_team/simulated/util/extra_kinetics/ExtraBlockPos.java` | Simulated `ExtraBlockPos` API shape, reduced local compile stub | Adapted | Simulated Project code license: MIT | Simulated Team / Creators of Aeronautics MIT notice | Notice added in `THIRD_PARTY_NOTICES.md`; replacing with dependency/API-only approach would still be cleaner |
| `src/main/resources/assets/simulated_pistons/textures/block/simulated_piston/piston_shaft.png` | Matches local Create Simulated `auger_shaft/auger.png` by SHA-256 | Copied | Simulated Project assets: All Rights Reserved | Permission from The Simulated Team / Creators of Aeronautics required | Needs manual review; replace or obtain explicit redistribution permission |
| `src/main/resources/assets/simulated_pistons/textures/block/simulated_piston/piston_frame.png` | Former placeholder was similar in purpose to Simulated `swivel_bearing.png`; hash differs from local upstream file | Adapted / Uncertain | Simulated Project assets: All Rights Reserved if derived | Permission required if copied or derived | Needs manual review; prove original source or replace |
| `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/parts/piston_link_plate.json` | Similar to Create Simulated/Create Aeronautics bearing plate concepts | Adapted / Uncertain | Upstream asset directories: All Rights Reserved if copied or derived | Permission required if copied or derived | Needs manual review; replace or document original authorship |
| `src/main/resources/assets/simulated_pistons/models/block/simulated_piston/parts/piston_shaft_axis_y.json` | Similar to Simulated auger/shaft assets | Adapted / Uncertain | Upstream asset directories: All Rights Reserved if copied or derived | Permission required if copied or derived | Needs manual review; replace or document original authorship |
| Other `src/main/resources/assets/simulated_pistons/models/**` | Project placeholder block models using local texture names and Create-style conventions | Original / Uncertain | Project MIT only if original; upstream assets all-rights-reserved if derived | Attribute upstream inspiration; preserve permissions for derived assets | Needs manual review for Blockbench/model provenance |
| `src/main/resources/assets/simulated_pistons/blockstates/**` | Project blockstate definitions | Original | Project MIT | None beyond project license | OK |
| `src/main/resources/assets/simulated_pistons/lang/en_us.json` | Project language entries | Original | Project MIT | None beyond project license | OK |
| `src/main/resources/data/**` | Project recipe, loot table, and tags | Original | Project MIT | None beyond project license | OK |
| Gradle wrapper files | Gradle wrapper distribution | Copied tool wrapper | Apache License 2.0 notice in wrapper comments | Keep wrapper notices intact | OK |
| `build.gradle`, `settings.gradle`, `gradle.properties` | Project build configuration | Original | Project MIT | None beyond project license | OK |
| `README.md`, `CHANGELOG.md`, `KNOWN_ISSUES.md`, `CONTRIBUTING.md`, `docs/**` | Project documentation | Original | Project MIT | Credit upstream projects where referenced | OK |

## Recommended Project License

Use MIT for original source code and documentation in this repository. This matches the existing Gradle metadata and is compatible with Create and Create Simulated/Create Aeronautics MIT-licensed code portions.

Do not claim all repository assets are MIT unless the asset provenance is resolved. The current `LICENSE` covers original code and documentation; the public README points readers to this audit for mixed-origin material.

## Public File Release Blockers

- Replace `piston_shaft.png` with an original or explicitly permissioned texture.
- Replace or verify provenance of `piston_frame.png`.
- Replace or verify provenance of piston link/shaft model parts.
- Re-run this audit after final art replacement.
