# Rust & Ruin 0.1.1-beta release readiness

November 14, 2026. This is the final readiness rerun for the gameplay patch, separate from the earlier expansion and version-only rebuilds.

## Current artifact

Replace the older JAR on **both client and server** with `rust-and-ruin-0.1.1-beta.jar`.

- Size: **3,557,756 bytes**.
- SHA-256: **`E83241B111FEB09F36DF8601247174CA54142372F3DFD4363F0DDA03070608FC`**.
- Final complete dedicated native suite: **423/423 passed**, zero failures.
- [Current evidence gallery, receipts, and retained failed attempts](../validation/release-ready-2026-11-14/index.html).

The previous D287 artifact and its 418-test receipts remain historical. The current package includes a final-shot infection fix; it must replace that earlier package even though the version remains 0.1.1-beta.

## Final-shot fix

A bow or crossbow that broke while firing could lose its Tetanus enchantment before the projectile received it. With Multishot, this also affected later projectiles in the same volley. Each actual projectile now reads the firing weapon's retained enchantment data, matches only Tetanus, and clamps its saved level to 0-2. There is no shared shot cache or state carried into an unrelated action.

Five added native regressions use real durability-consuming firing: the last bow arrow, the last crossbow arrow, every Multishot arrow, every Multishot firework, and ordinary trident/unbroken-volley controls. They check plain negative controls and saved projectile data; arrow/trident cases also perform accepted damage and verify the resulting disease. All five passed within the complete 423-case rerun.

## What was checked

The native suite covers recipes and acquisition routes, species-specific mutation eggs, diseases and cures, protection, saved-state migration, trades and scarcity, clinic/camp generation, real workstation accounting, traps, despawning, combat, boss rituals and rewards, and beacon payments. An earlier readiness repeat failed an egg-fixture count. The corrected test uses the center of its authored arena, snapshots existing UUIDs, requires exactly one newly created matching species with the correct mutation, and cleans only the new fixture entities. Its focused rerun and the subsequent complete suite passed; the failed receipt remains preserved.

All **16 final visible-client reports passed**: the broad plain appearance baseline, all 31 ordinary recipe transactions on a fresh actor, the causal advancement journey, forging, advancement layout, actual mixed/special Dragon rituals, six interaction modules, and medical/forge flows in both JEI and REI. The fresh crafting session has its own 62 original capture frames. The linked current manifest identifies each final run, original screenshots and disclosed fixtures separately from earlier attempts; an earlier pass is not substituted for final-candidate coverage.

The independent medical/worldgen/Survival review read every line of **38 production/generator files, 2,333 lines**, plus five native test files and five runtime JSON files, with exact hashes and read ranges. Its scope included rank-appropriate unique offers, scarce stock and resource-identity migration, saved delivery clocks, native Hero discounts, workstation ownership/reach and item accounting, five clinic biomes, native reference bounds, camp placement/access, cures, and Survival trap behavior. This bounded review found no additional confirmed production release blocker. Automated file-format and package checks are separate from manual semantic review.

## Limits

The isolated packaged-server probe verified discovery of 45 mods and namespace/mixin resolution up to Minecraft's **EULA=false guard**. It stopped without creating a world. It does not establish complete production-server initialization or remote multiplayer gameplay.

Graphical fixtures supply ingredients and use controlled mobs, camera flight, seeded approaches, accelerated waits, or invulnerability where disclosed. Boss completion fixtures verify full initial health before reducing the boss for a native final hit. They establish the recorded functionality, not a complete Survival balance playthrough. Existing generated chunks are not retrofitted with clinics or camps. Long-running public servers, arbitrary modpacks/resource packs, and external-client multiplayer remain outside this pass. These checks cannot prove the absence of every possible bug.

See [validation](../VALIDATION.md), [patch findings](PATCH_REVIEW_0.1.1-beta.md), [release notes](RELEASE_NOTES_2026-11-14.md), and [recipes](RECIPES.md).
