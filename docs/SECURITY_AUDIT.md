# Security and stability review

Latest: [October 25 playtest fixes and verification](PLAYTEST_FIXES_2026-10-25.md), with [two 322-test runs, current full JEI and focused graphical checks](../validation/playtest-2026-10-25/verification/results.json). Earlier playtest and full-project audits remain dated snapshots.

The latest review checked server-side reset permissions, native entity-interaction reach and line of sight, bounded captive lookup, exact captured UUID matching, and retained Creative/Spectator immunity. Infection audio scopes restore thread-local state in `finally` blocks and announce accepted new infections. Golem configuration is finite and bounded. Cave generation uses a bounded per-attempt column cache with no retained world state or forced chunk loads; native spacing and full fluid boundaries are tested. No additional confirmed security defect was found. Long-running remote-server and third-party advisory testing remain outside this review.

The new medic-menu message accepts exactly two primitive bytes, validates the native menu ID and boolean, and binds metadata to the originating connection and the actual current merchant-screen object. It cannot transfer items or perform a server action. Native server trading, stock consumption, reputation and Hero discounts remain authoritative. Review also covered trap capture state and teleport bounds, saved delivery clocks, legacy persistence, and medicine consumption.

The tested release contains no development/test classes or embedded recipe-viewer implementations. All source resources match the JAR; duplicate entries, unsafe paths and altered resources were rejected in three deliberate archive tests. The final evidence records the exact artifact hash. No additional confirmed security vulnerability was found in the changed code. This pass covers source/protocol/package checks; third-party advisory scanning and remote-server penetration/stress testing are separate work.

The sections below preserve earlier review snapshots.

Review scope: Rust & Ruin 0.1.0-beta expansion on Minecraft 1.20.1 Fabric. Automated checks support the findings below; this is not a claim that every possible mod combination or gameplay sequence is bug-free.

## Network and authority

- Existing configuration, symptom, and infection-presence decoders retain strict length/range/shape checks. Deliberately malformed messages pass through real client receivers during every smoke run without ending the run.
- Rabies presence reuses the fixed 17-byte UUID/boolean decoder, with a separate bounded 4,096-entry client cache and disconnect cleanup. Servers clear cached states on disconnect and stop.
- No new arbitrary client-to-server custom payload is introduced. Gloves use native inventory synchronization. The Creative extension accepts only slot 46, only from an actual Creative player, and only an empty stack or one Protective Gloves item; it enforces the server thread before mutation. Tests reject Survival, wrong-item, and oversized-stack updates.
- Operator mutation commands retain permission level 2, finite coordinate and world checks. `/give` changes only autocomplete; it does not change command permissions.
- Beacon selections remain server-validated for pyramid tier, primary/secondary pairing, and netherite payment. Invalid requests cannot consume payment or select stronger powers.
- Medicine, equip, rusting, mutation, loot, traps, and recipe consumption run on the server. Tonics reject wrong/empty stacks and spectator use, preserve Creative behavior, and account for bottles.

## Persistence and item integrity

- Enchantment level is captured when a projectile is fired, bounded to 0–2, and persisted. Switching held items after firing cannot change an in-flight shot's disease level.
- Rabies progression counters and loaded heart penalties are bounded. Modifier UUIDs prevent stacking from repeated save/load or conversion; the cure removes the temporary penalty. Death respawn clears Rabies.
- Glove storage is limited to one validated item, migrates old offhand saves, and handles keep-inventory, death drops, and Vanishing. Regression tests check duplication and NBT preservation.
- Batch arrow smithing checks ordinary arrows, Rust, and an empty template. Shift-click handles partial inventory space; full inventory consumes no ingredients. Normal take, repeated shift-click, and counts 1/16/64 are covered.
- Captured bear-trap victims survive conversion to the rusty block entity. Closed traps cannot retrigger until reset.
- Temporary Tetanus no longer changes ordinary animal loot. Permanent tetanized and rabid animal drop overrides respect `doMobLoot`; hostile drops retain their own path.
- Existing item IDs and legacy rusty iron tool behavior remain compatible. Natural mutation excludes command/egg/breeding spawns and special boss rituals.

## Build and package checks

- Official Gradle 8.6 wrapper checksum and distribution SHA-256 are pinned and verified.
- Every current source resource must match the packaged resource byte-for-byte, apart from deliberate version/refmap processing.
- Archive paths reject traversal, absolute paths, backslashes, duplicate entries, credential filenames, and bundled test/viewer implementation classes.
- JSON, PowerShell syntax, strict UTF-8, local documentation links, translation keys, compressed structure headers, model/texture references, and every item/status sprite margin are scanned.
- Clinic structure data is loaded and placed by a live server test; its generator is deterministic on repeat runs.
- No additional required mod dependency was introduced. JEI and REI each receive their own graphical runtime test; plain startup checks optional integration isolation.

## Practical limits

The review does not cover arbitrary third-party mixins, oversized external resource packs, every possible village terrain collision, or long-running public-server load. Clinics are weighted village house entries, so their presence is not guaranteed in a particular village. Existing generated terrain is unchanged. World-generation variety and combat balance still benefit from the owner's in-game testing. Player-owned projectiles continue to obey the server's PvP rules.

## October 19 follow-up

The movement-spasm hook now leaves non-finite positions and rotations to vanilla validation instead of canceling validation and issuing a teleport. A native handler regression checks NaN and both infinite rotation values. The full source review, persistence migration and disease-state fixes are documented in [the follow-up audit](DESPAWN_AUDIT.md).


## Maintenance-feature review

The [fresh source review](LINE_AUDIT.md) adds server-side delivery and veterinary-consumption checks, saved daily cooldowns, bounded operator diagnostics that do not load chunks, and coating data bounds. Existing packet fuzzing, invalid movement, permission checks and Creative glove-packet regressions are retained. File-audit annotations now describe automatic checks without implying those checks performed the separate manual review. This is not a new third-party dependency CVE assessment.
