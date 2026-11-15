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

# Historical boss/medical patch snapshot (before the final readiness rerun)

The boss/medical patch passed **418/418 native server tests** after the reported fixes and additional source review. Its current graphical checks and original screenshots are in the [patch gallery](../validation/boss-fixes-2026-11-14/index.html); the [patch review](PATCH_REVIEW_0.1.1-beta.md) maps all twelve requests to behavior and verification. REI final confirmation is pending.

Archived artifact: `rust-and-ruin-0.1.1-beta.jar`, **3,557,371 bytes**, SHA-256 **`D287996D5E8E73CE7C4C75CDEB186677EF4B3B328A5B779073505FF744A9348D`**. The [release receipt](../validation/boss-fixes-2026-11-14/final-release-verification.json) identifies this rebuilt gameplay patch. All **409 generated resources** were regenerated with identical bytes. Controlled graphical fixtures are disclosed; this is not a Survival balance playthrough.

The evidence below describes earlier artifacts and remains historical, including the earlier metadata-only rebuild with this same version number.

---

# Current expansion audit — November 14, 2026

**Historical version-only verification, superseded by the patch above.** The [metadata-only rebuild is verified](../validation/version-0.1.1-beta/verification.json): only the version in `fabric.mod.json` changed; all **267 compiled classes** and every other resource entry are byte-identical to the tested **0.1.0-beta baseline**. Archived artifact: `rust-and-ruin-0.1.1-beta.jar`, **3,501,095 bytes**, SHA-256 **`957F54966E70FFA9B3B27136D337EBF3D9A1E6551DF112D13DA39A7BC808BE2A`**. The original 399-test and graphical receipts retain their original metadata and hashes; no additional gameplay rerun was performed for this version change.

The expansion's graphical checks are complete: **12 reports and 318 original screenshots**, covering all 30 visible advancement nodes at four GUI scales, all 31 ordinary recipes, both custom workstation menus, JEI/REI displays, real mixed/special End rituals, automatic dragon carry/drop, actual combat contribution, native XP/rewards, and level-IV beacon payments. See the [gallery](../validation/expansion-2026-11-14/index.html), [manifest](../validation/expansion-2026-11-14/manifest.json), and [findings](REVIEW_FINDINGS_2026-11-14.md) for the disclosed fixtures and partial-session history.

The independent new-dragon manual review read every line of **24 files/696 lines** in its source snapshot. Its [per-file hashes/ranges](../validation/expansion-2026-11-14/reviews/dragon-independent-review-2026-11-14.json) and [separate health/controller peer review](../validation/expansion-2026-11-14/reviews/dragon-health-controller-peer-review.json) have distinct scope. Native Minecraft bytecode was consulted locally, not published. These records do not claim a full manual semantic review of every existing Java file.

An automated inventory checkpoint passed for **1,844 nonignored files**, including 276 Java files checked for encoding/static translation references. JSON parsing, image decoding, PowerShell syntax, local links, and hashes are automated format/integrity checks. Compilation/runtime and manual semantic review are recorded separately. That checkpoint predates the final review copies and receipts.

Packaging and resource-refresh checks passed for the **0.1.0-beta baseline's 3,501,095-byte** JAR, SHA-256 **`7F1D6F6AB1C4001A7C09EFF7D3CD839345BB4F4CE2B9D56D494D4BD714B8AA25`**. **Final native validation passed all 399 tests with zero failures** in a complete 9-minute-24-second run. The preceding run's 398/399 result came from a positive-contact fixture still protected by native join invulnerability. The corrected test first checks the protected negative case, clears the test-only timer, and checks the positive hit; production spawn protection was retained. The tested gameplay baseline is ready for owner playtesting.

The current findings distinguish real lifecycle, health-cap, item-preservation, cure, XP, and crop-placement defects from older test assumptions and test-controller bugs. They do not assert universal bug freedom, arbitrary modpack compatibility, or long-running public-server coverage.

The following sections are earlier dated audit snapshots.

# Complete project audit for user testing

The October 19 follow-up reviewed all project source again and fixed the variant despawn fault, legacy saved persistence, weaker infection replacement, duration limits, movement validation, and command accounting. See the [follow-up audit](DESPAWN_AUDIT.md) and [source inventory](../validation/source-review.json).

This audit covers the Rust & Ruin 0.1.0-beta expansion based on local commit `e2a2b39`. The owner authorized committing the tested expansion on October 18, 2026.

## Issues found and corrected

- Batch arrow shift-click initially left arrows in the base slot. It now consumes only the transferred arrows and one Rust, including partial inventory space; a full inventory consumes nothing.
- Rusting a closed bear trap discarded its captured victim. Conversion now carries the block entity's saved state across, allowing the reset tool to release the original victim.
- A Rabies vaccine check on ordinary attacks alone missed direct potion effects. Prevention now runs once at status application, including drinkable/splash exposure.
- The upward-nail advancement was attached to the general living-entity death method. Server players use a separate method; the advancement now runs on actual player death and has positive/negative regressions.
- Creative inventory positioned the extra glove slot over the hotbar and rejected its synchronization. The extra slot now has a dedicated position and a strictly validated native Creative packet extension.
- Standalone Lockjaw needed to survive cleanup that was intended for recovered Tetanus. It now persists until its own timer or antitoxin treatment; milk preserves it.
- Tetanus II now retains its stronger symptoms for the infection duration. Antitoxin clears them, and milk cannot remove those symptoms while Tetanus II remains.
- Tetanus I/II are restricted to combat weapons. Native anvil tests reject nails, hooklines, pickaxes, shovels, and hoes.
- Rabid mutations now survive native mob conversion without stacking their extra health.
- Barbed wire cannot apply Slowness to spectators. Nail item use enforces its cooldown internally.
- Clinic medics now retain their profession while finding their workstation. The server test checks after 160 AI ticks, and the graphical test follows the same medic by UUID.
- The client harness now reports failures from every queued server task, preventing a later screenshot or shutdown from masking a failed asynchronous assertion.

Earlier beacon selection validation, scoped hookline infection, reflected Thorns exclusions, antitoxin bottle accounting, bounded custom packets, and command permission checks remain covered.

## Coverage

`tools/Audit-Files.ps1` reads every tracked and new nonignored file, validates its applicable format, and records size and SHA-256. Intentionally deleted source files are listed separately. Generated caches, downloaded tools, Git internals, and game saves are excluded. The per-file report stays at `.tools/expansion-file-audit.json` to avoid hashing its own changing inventory.

The source review covers initialization, diseases, saved state, prevention, items, projectiles, native and Creative glove synchronization, loot, mutation, AI, trades, village pools, traps, beacon validation/UI, commands, rendering, and optional viewers. All production and test Java is compiled. Automated file checks are format/integrity checks; they are not a proof of semantic correctness for every possible execution.

Every JSON is parsed; Gradle and live Minecraft check recipes, tags, loot, advancements, models, metadata, and mixins. PNGs decode successfully, and all item/status sprites have transparent borders. Original MP3 and packaged Vorbis audio decode with FFmpeg. PowerShell syntax and UTF-8 are checked, local Markdown links resolve, and full resource regeneration exercises the sprite isolator. The compressed clinic template loads and places in Minecraft, replaces its road connector, and regenerates deterministically.

The release auditor compares every source resource to the JAR, validates processed metadata/refmaps, checks archive paths and duplicates, verifies the official Gradle wrapper and pinned distribution, and rejects test/viewer implementation classes and private files. Disposable altered, duplicate-entry, and absolute-path JARs are rejected as expected; see [negative checks](../validation/package-negative.json).

## Evidence and limits

See [VALIDATION.md](../VALIDATION.md), [server XML](../validation/gametest-results.xml), [repeat XML](../validation/gametest-results-repeat.xml), [client results](../validation/client-results.txt), and [release audit](../validation/release-audit.json).

Automated tests and review reduce risk. They do not prove 100% correctness or cover arbitrary modpacks, every village terrain layout, every network condition, or long-running public-server load. Development runtime tests exercise the mod on dedicated and integrated servers; the remapped release JAR receives separate packaging/integrity checks. User playtesting of the release artifact remains the next step.

Prioritize multiplayer combat and prevention, full-duration Tetanus II, Rabies progression/cure, glove death/logout behavior, naturally generated clinics, trap reset/rusting, batch smithing, and beacon payments. These playtesting priorities remain after the authorized commit.


## Fresh numbered review and maintenance features

The current review covers 166 source/build files and 8,950 lines. See [LINE_AUDIT.md](LINE_AUDIT.md) for actual read ranges, new findings, feature behavior and limits, plus [line-review.json](../validation/line-review.json) for the per-file hashes and read receipts. Automated file-format checks are recorded separately.

Final November 14 checkpoint: [1,854 files passed](../validation/expansion-2026-11-14/reports/file-audit-final.json), and [408 generated resources reproduced identical bytes](../validation/expansion-2026-11-14/reports/resource-repeatability.json). These are automated integrity/format checks, separate from the bounded manual review above.
