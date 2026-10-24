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
