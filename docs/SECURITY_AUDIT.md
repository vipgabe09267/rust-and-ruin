# Final 0.1.1-beta readiness rerun - November 14, 2026

The final complete native server suite passed **423/423 tests**, including five new regressions for Tetanus-enchanted bows/crossbows breaking on their firing shot and for every projectile in a Multishot volley. The fix preserves the actual firing weapon's retained enchantment data without a global shot cache; plain and saved-projectile controls are included.

Current artifact: `rust-and-ruin-0.1.1-beta.jar`, **3,557,756 bytes**, SHA-256 **`E83241B111FEB09F36DF8601247174CA54142372F3DFD4363F0DDA03070608FC`**. Replace the older JAR on both client and server. The [final evidence gallery](../validation/release-ready-2026-11-14/index.html) and [readiness scope and limits](RELEASE_READINESS_0.1.1-beta.md) distinguish the current candidate from earlier artifacts.

All **16 final visible-client reports passed**: the plain baseline, 31-recipe crafting session, causal advancement journey, forging, layout, actual mixed/special Dragon rituals, six interaction modules, and medical/forge flows in both JEI and REI. The linked gallery retains their original screenshots and disclosed fixtures. The packaged-server probe stopped at the EULA=false guard and did not create a production world. Remote multiplayer/modpack endurance and a full Survival balance playthrough remain outside this pass.

The sections below are historical snapshots for their own artifacts and run stages. Their older hashes, counts, and then-pending checks are retained as history, not the current verdict.

---
# Historical boss/medical patch snapshot (before the final readiness rerun)

The boss/medical patch passed **418/418 native server tests** after the reported fixes and additional source review. **Eight fresh graphical reports and 81 original screenshots** cover the redesigned beacon, medical trades, actual Wither/Dragon encounters and JEI/REI workstation flows. The [patch gallery](../validation/boss-fixes-2026-11-14/index.html) preserves the evidence; the [patch review](PATCH_REVIEW_0.1.1-beta.md) maps all twelve requests to behavior and verification.

Archived artifact: `rust-and-ruin-0.1.1-beta.jar`, **3,557,371 bytes**, SHA-256 **`D287996D5E8E73CE7C4C75CDEB186677EF4B3B328A5B779073505FF744A9348D`**. The [release receipt](../validation/boss-fixes-2026-11-14/final-release-verification.json) identifies this rebuilt gameplay patch. All **409 generated resources** were regenerated with identical bytes. Controlled graphical fixtures are disclosed; this is not a Survival balance playthrough.

The evidence below describes earlier artifacts and remains historical, including the earlier metadata-only rebuild with this same version number.

---

# Current changed-scope review — November 14, 2026

**Historical version-only verification, superseded by the patch above.** The [metadata-only rebuild is verified](../validation/version-0.1.1-beta/verification.json): only the version in `fabric.mod.json` changed; all **267 compiled classes** and every other resource entry are byte-identical to the tested **0.1.0-beta baseline**. Archived artifact: `rust-and-ruin-0.1.1-beta.jar`, **3,501,095 bytes**, SHA-256 **`957F54966E70FFA9B3B27136D337EBF3D9A1E6551DF112D13DA39A7BC808BE2A`**. The original 399-test and graphical receipts retain their original metadata and hashes; no additional gameplay rerun was performed for this version change.

The expansion review covered server-authoritative workstation transactions and ownership/reach/spectator checks, medic-offer migration, consumable advancement criteria, bounded dragon damage/reward state, boss minion limits and player-item preservation, and beacon payment validation. The independent new-dragon review found and corrected an interrupted-grab reload lock and incorrect native death-layer handling; separate work corrected the 1,024-health clamp, native XP/exposure, and minion item pickup. See [confirmed findings](REVIEW_FINDINGS_2026-11-14.md) and [bounded per-file review snapshots](../validation/expansion-2026-11-14/reviews/README.md).

Actual plain/JEI/REI menus and the full native End ritual/contribution/reward path passed graphically. Full-inventory refusal, closing previews, repeated IV payments, unused currency returns, offline/repeated reward handling, and custom recipe boundaries have targeted checks. The [318-frame gallery and reports](../validation/expansion-2026-11-14/index.html) disclose invulnerability/ingredient/AI fixtures; their successful functional scopes do not establish penetration-test or remote-load coverage.

[Packaging](../validation/expansion-2026-11-14/reports/release-final.json) and [resource-refresh](../validation/expansion-2026-11-14/reports/refresh-final.json) checks passed for the **0.1.0-beta baseline**, SHA-256 `7F1D6F6AB1C4001A7C09EFF7D3CD839345BB4F4CE2B9D56D494D4BD714B8AA25`. An automated inventory checkpoint covered 1,844 files before the final review copies and receipts. **The final native suite passed all 399 cases with zero failures.** The preceding 398/399 result was a newly added positive-contact fixture still under native join immunity. The corrected fixture first verifies rejection while protected, then clears only the test timer for the real positive hit. No production spawn immunity was removed. The tested gameplay baseline is ready for owner playtesting; this is not remote-server penetration or modpack certification.

No additional confirmed arbitrary-output, inventory-duplication, ritual-ordering, repeat-reward, or damage-credit vulnerability was found in the 24-file/696-line independent snapshot. That bounded finding does not certify all 276 Java files as manually audited or prove the absence of all vulnerabilities. Earlier findings below describe their own dated source/artifacts.

# Security and stability review

Latest: [clinic/advancement review](CLINIC_ADVANCEMENT_FIXES_2026-10-25.md), with [two 341-test runs and actual graphical records](../validation/clinic-advancements-2026-10-25/verification/results.json). Operator-only locate aliases retain permission and target allowlist checks; no retrogen or new packet handler was added. Earlier findings remain dated snapshots.
The latest additions retain native operator level 2, argument whitelists, source feedback/silent policy and coordinate-link styling for branded locate aliases. They read only the already planned structure-start chunk for an entrance hint and introduce no custom client-to-server payload. Per-attempt cave caches are bounded; accepted fallback sites get full native terrain/fluid validation. Only explicitly tagged camp guardians gain persistent patrol homes. Positional infection/work packets are bounded to 16/12 blocks respectively, and nested silent scopes restore state in finally blocks. Native listener boundaries, spawn/load/conversion silence, placement stages, clipping, bed/job claims, unwanted water and exact 169-block floor integrity are covered. No new confirmed security defect was found in this scope. Substantial temporary noise-generation allocation remains a performance limitation.

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
