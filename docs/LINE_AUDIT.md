# Numbered source audit - October 19, 2026

Rust & Ruin remains **0.1.0-beta**, for Minecraft 1.20.1 Fabric. This pass implements the six suggested features and reviews the resulting project source. Changes remain local for owner testing.

## What was read

I read the executable project source in numbered sections: **166 files and 8,950 lines**, including production Java, every GameTest and client harness, PowerShell generators/auditors, C# sprite extraction, Gradle build/settings, both wrapper launchers, and Gradle properties. Changed sections were reread after fixes. The [per-file record](../validation/line-review.json) contains line ranges, final SHA-256 hashes, review notes, and 185 read receipts. Raw numbered transcripts remain locally in `.tools/review-reads/`.

One tool response truncated the beginning of ProgressionExpansionGameTests. Its missing lines were explicitly reread in transcript 24. Review receipts preserve hashes at the time of reading; the final inventory records the finished files. Bookkeeping compares final source text with the captured reads and flags unseen changed lines. That comparison records coverage; it does not perform the manual reasoning.

Generated JSON and binary assets were checked through parsers, resource references, image decoding/margins, regeneration, package comparisons, and native game loading. Third-party library/JDK/build-cache source is outside this manual review. No claim is made that reading every line proves absence of all bugs.

## Six features implemented

| Feature | Result and boundaries |
| --- | --- |
| Spawn settings | Separate Easy/Normal/Hard settings for natural animals, other natural mobs, and spawners. Retains 10/15/20% natural and 25/35/45% spawner defaults. Peaceful remains zero. Contact mutation rates remain separate. The guide displays current server values. |
| Medic supply requests | Four rotating requests, one accepted delivery per medic per world day, saved cooldown, exact material consumption and honey-bottle returns. Rewards native villager reputation, actual lower medicine prices, and Just What the Doctor Ordered. Ordinary reputation decay, stock, demand and price limits remain. |
| Mob inspector | Operator-only `/rustandruin inspect <entity>` explains variant/persistence state. `/rustandruin population [1-128]` counts loaded nearby mobs, with 64 blocks by default. No chunk loading or entity removal. |
| Rust-proof coating | One honeycomb per item in the opposite hand coats supported clean metal. Saved item NBT gives 12000 wet-weather ticks by default; durability use spends 20 coating ticks per damage point. It expires, can be reapplied, preserves ordinary NBT, and has JEI/REI displays. It protects dropped equipment, not placed traps. |
| Animal treatment | Single-use Veterinary Kit cures permanent tetanized/rabid animals, restores normal attributes, texture, movement, drops and behavior, and grants Clean Bill of Health. It preserves native ownership/persistence. Healthy animals, hostiles, empty/wrong stacks and spectators cannot consume treatment. |
| Abandoned workshops | Natural jigsaw structures in eight temperate Overworld biomes, with rusty traps, worn equipment, modest salvage and occasional medicine. Includes biome tag, placement set, template pool, structure NBT and loot table. New chunks only; no added spawners. |

Exact recipes, settings, costs, trades and loot are in [Recipes](RECIPES.md) and [README](../README.md). Modrinth copy is updated in [MODRINTH_DESCRIPTION.md](../MODRINTH_DESCRIPTION.md).

## Findings fixed during this fresh pass

| Finding | Fix and evidence |
| --- | --- |
| Salmonella symptoms ran only while duration was below 1200 ticks | Removed that duration ceiling while retaining the 200-tick interval. A native StatusEffectInstance regression verifies both a 6000-tick effect and an infinite effect at age 2400 cause vomiting/hunger loss. Non-interval ticks stay inactive. |
| REI coating output could retain the pre-connection configuration duration | Coated output entries are now created from current synchronized settings when requested. JEI already creates the output dynamically. Both viewers register nine conversions and open the actual coating category in the client harness. |
| New coating hook queried weather for every dropped item | Guarded environmental checks behind supported-metal eligibility. Ordinary non-rustable drops skip that work. Existing water/rain rusting, coating expiry and fishing salvage tests still pass. |
| Repeated expansion generation duplicated the command-guide appendix | Replaced appending with assignment of a complete command chapter. Two repeat generations produce identical resource output. |
| Language key enumeration produced byte-order churn on regeneration | Sort language keys before serialization. All 330 source resources are byte-identical across repeated expansion generation. |
| New spawn controls left the creature guide displaying hard-coded rates | Added nine synchronized configuration arguments for natural animal, other mob and spawner values. The existing contact-rate arguments remain separate. |
| Build helper kept an invalid JAVA_HOME when Java existed only on PATH | Clear the invalid value when choosing PATH Java. An isolated wrapper fixture reproduces failure with the old script and succeeds with the fixed script. No project folders were moved or deleted. |
| Automatic file-audit labels implied that format scans proved manual review or compilation | Changed labels to report the automated operation only. Manual review receipts and actual compiler/runtime results are now recorded separately. |
| Low-contrast REI coating instructions | Set an explicit dark text color and visually checked the actual recipe screen. |

## Test-fixture corrections

These were test problems, not demonstrated gameplay bugs:

- A delivery test assumed the hand must become empty. Returned glass bottles can occupy that slot; the corrected assertion checks the requested supplies were consumed, and separately verifies actual trade discounts and saved cooldown rejection.
- A coating test let the nail float out of its shallow water. A deeper column and disabled pickup now maintain the intended exposure and verify rusting after protection expires.
- The workshop screenshot camera fell below its small platform. The approach platform now extends to the camera and an assertion rejects a camera below the structure. The retained screenshot is visually inspected.

## Previously found stability fixes rechecked

This pass retains and reruns the [despawn audit](DESPAWN_AUDIT.md): no forced mutation persistence; conservative one-time legacy repair; player-kept creature guards; weaker/infinite infection preservation; Lockjaw continuity; configured long durations; bounded combined-arrow duration; vanilla malformed-movement rejection; and accurate command conversion counts. These are earlier findings, not newly rediscovered defects.

Animals retain vanilla species/ownership rules. A cow that would normally remain in the world is not removed simply because it is infected. Previously unnamed, deliberately persistent legacy mobs cannot always be distinguished from the old forced flag; recognizable ownership/name/equipment reasons are protected. See the despawn report for migration limits.

## Validation and remaining limits

- Three full post-audit dedicated-server runs and a fourth release recheck: **192 required GameTests each, zero failures**, including native cure loot/advancement behavior and native workshop world-generation starts.
- Real graphical clients without viewers, with JEI 15.3.0.4, and with REI 12.1.725: recipe registration, actual recipe screens, disease tooltips, 78 base mutation skins, 22 rabid skins, HUD/tab hearts, glove synchronization, guide chapters, beacons, advancements, kit recipe and workshop rendering.
- Full resource regeneration compiles/exercises the sprite helper. All structured resources, image margins, recipe outputs, translations and source-to-package bytes are checked separately.
- Source MP3 and packaged OGG are decoded with FFmpeg. Deliberately tampered resources, duplicate JAR entries and absolute archive paths are rejected by the release auditor.
- Operator permission/radius checks, malformed packet fuzzing, invalid movement, native Creative glove packets, full/partial inventory behavior, save/load and repeated application remain covered.

Current run reports and hashes are linked from [VALIDATION.md](../VALIDATION.md). This is substantial regression coverage, not a guarantee of 100% correctness. Long multiplayer soak tests, every possible world seed/terrain edge, and compatibility with unrelated inventory/combat/world-generation mods still require player testing. Naturally generated structures are tested through Minecraft's structure-start path and template placement; every terrain combination is not exhaustively generated.

## October 21 verification supplement

The [fresh complete check](../validation/recheck-2026-10-21.json) repeats server, client, resource, audio and package checks. It also fixes REI instruction contrast in both themes and wraps clipped rusting/coating text. The original manual review above remains dated October 19: 165 files retain those hashes; the complete changed 118-line REI file was read again and fingerprinted in the supplement. Current source totals 8,958 lines across the same 166 files.
