# Current full reaudit - October 23, 2026

A fresh manual review covered **193 executable files and 10,632 lines**. It reproduced and fixed **17 defects**, including pack-animal cargo loss, End-return disease loss, trap contact errors, cabinet pathfinding, glove synchronization/animation, and native Wither ritual incompatibilities. The [complete findings](docs/REAUDIT_2026-10-23.md) link to before-fix evidence and the [per-file review manifest](validation/reaudit-2026-10-23/line-review.json).

The final **251-test server suite passed twice**, including a forced rebuild. Final no-viewer, JEI, REI LIGHT and REI DARK clients passed; all 357 resources regenerated identically twice, and the forced rebuild produced the same JAR hash. Package, negative archive, audio, file and whitespace checks passed. See [current results](validation/reaudit-2026-10-23/results.json). The owner authorized a local commit on October 24, 2026 while continuing Survival/multiplayer testing; pushing remains deferred. These checks do not guarantee the absence of every bug or replace a remote multiplayer playtest.

The sections below preserve earlier snapshots and their historical counts.
# Current refresh verification - October 23, 2026

The latest 24-request refresh has its own [change matrix and review](docs/REFRESH_2026-10-23.md) and [evidence directory](validation/refresh-2026-10-23/line-review.json). The full 231-test server suite passed twice; JEI, REI light/dark and no-viewer clients passed. All 357 resources regenerated identically. Package and whitespace checks passed. Owner survival/multiplayer testing remains required before release; no commit or push was made for this refresh.

The sections below preserve earlier validation snapshots and their historical counts.

# Rust & Ruin 0.1 validation
Validated October 23, 2026 with Minecraft 1.20.1, Java 17, Fabric Loader 0.16.14, Fabric API 0.92.6+1.20.1, **Loom 1.10.5 and Gradle 8.14.6**. Version remains **0.1.0-beta**. Changes are local and uncommitted for owner testing.

## October 23 results

- Fresh manual numbered review: **167 source/build/test/generator files, 9,159 lines**. All 128 production Java files were read anew. Final hashes match complete read ranges; every modified source file was reread. See [per-file coverage](validation/line-review-2026-10-23.json) and [findings, fixes, remaining risks, and feature suggestions](docs/AUDIT_2026-10-23.md).
- Added **13 regression/boundary tests**. The first eight reproduced failures before fixes; the prior 192 tests passed in that run. The corrected expanded suite passes **205/205**, including repeated full task execution with zero failures or skips.
- Graphical client runs: **PASS** without viewers, with JEI 15.3.0.4, and with REI 12.1.725 in LIGHT and DARK themes. Verified synchronized diseases/gloves, 78 mutation render layers, 22 rabid skins, 12 potion tooltips, guide, advancements, beacon, creative inventory, structures, and viewer recipe registration. Inspected fresh recipe screens and representative HUD, beacon and glove screens; this is not a claim that every captured image was manually examined.
- All **330 resources regenerate byte for byte** and match the release package. Strict file/encoding/translation/image/link checks, source MP3 and packaged OGG decoding, official wrapper/distribution checksums, and release packaging checks pass. Altered resources, duplicate archive entries, and unsafe paths are rejected by three negative package tests.
- The build helper's invalid-JAVA_HOME fallback passed its isolated PATH-Java fixture. The REI theme configuration was restored byte for byte after the dark run. The final metadata-expansion build deprecation was removed.

The artifact is `build/libs/rust-and-ruin-0.1.0-beta.jar`, **3,187,130 bytes**, SHA256 `15F6584359F5B19529680AF35C256EDFFDBEE849A8551A8A7037DDE04E8D471B`. Repeated full builds produced the same hash.

Evidence: [failed regressions before fixes](validation/audit-2026-10-23/regressions-red.xml), [expanded server run](validation/audit-2026-10-23/server-first.xml), [final server repeat](validation/audit-2026-10-23/server-repeat.xml), [plain client](validation/audit-2026-10-23/client-plain.txt), [JEI](validation/audit-2026-10-23/client-jei.txt), [REI LIGHT](validation/audit-2026-10-23/client-rei.txt), [REI DARK](validation/audit-2026-10-23/client-rei-dark.txt), [release integrity](validation/audit-2026-10-23/release.json), and [negative package tests](validation/audit-2026-10-23/package-negative.json). The dated evidence directory contains nine fresh viewer screenshots. Full logs, numbered source-read receipts, and the remaining smoke screenshots are retained locally under ignored tool/build directories.

This audit exercises a dedicated GameTest server and integrated clients. It does not prove the absence of every bug or compatibility with every modpack, resource pack, permissions extension, or long-running public server. The report identifies those limits separately from reproduced defects.

## Earlier validation: October 21

Validated October 21, 2026 with Minecraft 1.20.1, Java 17, Fabric Loader 0.16.14, Fabric API 0.92.6+1.20.1, Loom 1.6.12, and Gradle 8.6. Version remains **0.1.0-beta**. The expansion was committed on October 18; the October 19 stability fixes, maintenance additions and October 21 REI display fixes remain uncommitted for owner testing.

## Results

- Dedicated server: **192 required GameTests passed in each of six fresh October 21 runs**, with zero failures or skips. Run 1 forced all tasks; run 6 follows the final REI fixes.
- Graphical clients: **PASS** without a recipe viewer, with JEI 15.3.0.4, and with REI 12.1.725. The final REI instructions were also checked in both LIGHT and DARK themes.
- Release build and resource validation: **PASS**. All source resources match the packaged artifact, including the redesigned medic and clinic.
- File audit: all tracked and new nonignored project files pass their applicable format, encoding, image, translation-reference, or local-link checks. All 597 files passed; the complete inventory is retained locally at `.tools/recheck-2026-10-21-final-files.json`.
- Package auditor rejects deliberately altered resources, duplicate entries, and absolute archive paths. No test classes, recipe-viewer implementations, private files, or unsafe paths are bundled.

Evidence: [October 21 recheck](validation/recheck-2026-10-21.json), [first server run](validation/gametest-results.xml), [final server run](validation/gametest-results-repeat.xml), [server summary](validation/server-runs.txt), [client results](validation/client-results.txt), [release integrity](validation/release-audit.json), and [negative package tests](validation/package-negative.json). Fresh logs remain under ignored `.tools/recheck-2026-10-21-*.log` files; earlier audit logs are retained.

## October 19 source and despawn follow-up

The follow-up removes forced persistence from both mutation paths and repairs affected older saves. Added checks cover native despawning, mob-cap eligibility, preservation of player-kept creatures, weaker/infinite infection handling, duration limits, invalid movement during spasms, and command counts. See the [complete findings](docs/DESPAWN_AUDIT.md) and [source inventory](validation/source-review.json).

## Functional coverage

The 192 tests cover diseases, treatment, protection, item use, damage, rusting, recipes, variants, loot, projectiles, advancements, commands, saved state, traps, villagers, and beacon behavior. Specific expansion regressions include:

- Native anvil application of Tetanus I/II to combat weapons; rejection of nails, hooklines, and mining tools. Actual bow, crossbow, and trident projectiles retain the weapon infection level.
- Tetanus II symptoms throughout the infection, antitoxin cleanup, milk resistance, standalone Lockjaw persistence, Rabies progression/cure/vaccination, and infected versus permanently mutated animal loot.
- Actual shield prevention, glove prevention, antitoxin recovery, and upward rusty-nail death advancement conditions, including negative cases.
- Batch arrow smithing for 1, 16, and 64 arrows; partial and full inventory handling without duplication or undeserved ingredient consumption.
- Dedicated glove-slot synchronization, persistence, death handling, and validation of native Creative inventory packets. Incorrect items, counts, and player modes are rejected.
- Bear-trap capture/reset, captured-victim persistence across rusting, and spectator exclusion from barbed-wire effects.
- Natural mutation difficulty rates, initialization, variant eggs, attributes, drops, saved state, native mob conversion, and Rabid attacks against a real server-player fixture.
- Clinic template loading and placement, road-connector replacement, workstation and medic presence, profession retention after 160 AI ticks, village pool entries, and trades.
- A physically built full beacon, level III selection, automatic resistance, payment and base rules, and separate mod advancements.

Earlier regression coverage remains for bounded configuration and network messages, malformed payloads, command permissions, prevention boundaries, vaccine bottles, damage attribution, scoped callbacks, recipe outputs, salvage, vomiting, floating nails, interrupted water/rain exposure, and item-data preservation.

## Graphical verification

Each client harness enters a real integrated-server world, checks synchronized state, opens relevant screens, captures images, and exits with a fresh assertion result. Gradle removes stale result files before launch and rejects missing or failed results. Queued server-task failures propagate into the harness result.

Checks include nine Rusting displays, crafting and brewing, caltrops, batch arrow smithing, 78 vanilla mob base skin layers, 22 Rabid skins, twelve disease potion tooltips, rusty and Rabies HUD/tab hearts, Lockjaw, the custom beacon, advancements, the dedicated glove slot, Creative item coverage and synchronization, vaccination, and all ten field-guide chapters. Deliberately malformed custom messages are rejected while subsequent checks continue.

The redesigned medic and clinic were inspected in-game. The medic has a white coat, green cuffs/gloves and a cap, with visible eyes. The clinic has a timber gable roof, covered porch, lanterns, chimney, glazed windows, a medical banner, beds, a basin, and medicine storage.

Screenshots:

- [Medic](validation/screenshots/rust-and-ruin-plain-medic.png)
- [Clinic exterior](validation/screenshots/rust-and-ruin-plain-clinic-traps.png)
- [Clinic interior](validation/screenshots/rust-and-ruin-plain-clinic-interior.png)
- [JEI arrow smithing](validation/screenshots/rust-and-ruin-jei-arrow-smithing.png)
- [REI arrow smithing](validation/screenshots/rust-and-ruin-rei-arrow-smithing.png)
- [Beacon](validation/screenshots/rust-and-ruin-rei-beacon.png)
- [Creative glove slot](validation/screenshots/rust-and-ruin-plain-creative-gloves.png)
- [Rabies hearts](validation/screenshots/rust-and-ruin-plain-rabies-hud.png)
- [Advancements](validation/screenshots/rust-and-ruin-rei-advancements.png)
- [Field guide](validation/screenshots/rust-and-ruin-jei-field-guide.png)

## Reproduce

With JDK 17 from the project root:

```powershell
.\gradlew.bat --offline build runGameTest --console plain
.\gradlew.bat --offline build runGameTest --console plain
.\gradlew.bat --offline runClientSmoke --console plain
.\gradlew.bat --offline -PwithJei runClientSmoke --console plain
.\gradlew.bat --offline -PwithRei runClientSmoke --console plain
.\tools\Audit-Release.ps1
.\tools\Audit-Files.ps1 -Report .tools/expansion-file-audit.json
```

Offline commands require cached dependencies; omit `--offline` for an initial setup. Run viewers sequentially. The harness uses `build/client-smoke/saves/smoke-world-final`; if absent, first copy the generated GameTest world there.

## Artifact

`build/libs/rust-and-ruin-0.1.0-beta.jar`:

- Size: **3,185,156 bytes**.
- SHA-256: `646183D6A2927002DA5707AD4F042BBC781432731E31BA81919224DDEADB4CD6`.
- Includes 62 PNGs, audio, recipes, advancements, tags, item/block models, the clinic and workshop structures, and required mixins.
- Requires Minecraft 1.20.1, Java 17, Fabric Loader 0.16.14+, and Fabric API on both client and server. JEI and REI are optional.
- Runtime item/status sprites have transparent margins. Editable expansion artwork and building sources are documented in [art provenance](assets-source/art/EXPANSION_ART.md).

## Limits and next testing

These are dedicated/integrated-server functional tests, source/protocol review, adversarial packet tests, and separate release-package checks. They do not establish 100% correctness, arbitrary modpack compatibility, every village terrain layout, or long-running public-server stability. Development runtime tests and the packaged release audit serve different purposes; owner playtesting of this JAR is still required.

Prioritize multiplayer combat, full-duration diseases, glove logout/death handling, naturally generated clinics, trap reset/rusting, partial-inventory smithing, and beacon payment. Subjective appearance, sound volume, and difficulty balance also benefit from playtesting.

See [request audit](docs/REQUEST_AUDIT.md), [security review](docs/SECURITY_AUDIT.md), and [complete audit](docs/FULL_AUDIT.md).

## Earlier requested complete verification repeat (before maintenance features)

The earlier 173-test check set was run again at the owner's request on October 19: a forced rebuild, three additional 173-test dedicated-server runs, plain/JEI/REI clients, all reviewed source hashes, the file audit, release/resource checks, three intentionally damaged archives, and the Git whitespace check passed. Both audio files decoded without errors, and the C# sprite helper compiled and extracted a real atlas sprite successfully. No gameplay source changed during this repeat. The rebuilt artifact retained its recorded SHA-256.

## October 19 numbered audit and maintenance features

All six additions are implemented: separate animal/other-mob/spawner settings, daily medic supply requests, bounded operator diagnostics, honeycomb coating, veterinary treatment, and abandoned workshops. See [LINE_AUDIT.md](docs/LINE_AUDIT.md) for the exact behavior, fixes, fixture corrections and remaining limits. The [per-file review](validation/line-review.json) records 166 source/build files and 8,950 lines, with hashes, ranges and review notes. Automated file auditing is recorded separately.

Added coverage includes actual medic price reductions, saved cooldown and repeat-delivery rejection, wrong/insufficient materials, per-item honeycomb consumption, coating save/load/use/weather expiry, cure goal/attribute cleanup, restored ordinary cow loot, treatment advancement, operator permission/radius bounds, native workshop world-generation starts and template placement, and long/infinite Salmonella symptom ticks. Both recipe viewers also open the actual coating display and expose the Veterinary Kit recipe. Full resource regeneration exercises the C# sprite helper; repeat expansion generation yields 330 byte-identical resources.

## October 21 complete check

Rebuilt the project, repeated the full server suite six times, and completed ten graphical client runs while checking the recipe viewers. All 16 Gradle runtime runs succeeded without error-level/fatal log entries. Both audio files decoded without errors. Full resource regeneration exercised the C# helper; all 330 resources remained byte-identical after both full and repeated expansion generation. The isolated build-helper fallback and three damaged-archive rejection checks passed again.

Fresh screenshots revealed low-contrast REI rusting/arrow instructions and clipped rusting/coating text. Custom labels now use separate light/dark colors; long instructions wrap inside their panels, with extra height for rusting. The final edited REI class passed compilation, both theme runs and another full server suite. See [light rusting](validation/screenshots/rust-and-ruin-rei-rusting.png), [dark rusting](validation/screenshots/rust-and-ruin-rei-dark-rusting.png), and [dark coating](validation/screenshots/rust-and-ruin-rei-dark-wax-coating.png). Fifty-five fresh screenshots are fingerprinted in the dated report.

This repeat verified all 166 original source hashes before testing. After the REI-only fixes, 165 files still match the October 19 review. The complete changed file, now 118 lines, was read in two numbered sections; its final hash and receipt are recorded in the [October 21 supplement](validation/recheck-2026-10-21.json). The older 8,950-line review remains a historical snapshot; current source totals 8,958 lines. This check does not claim a new full manual reread of every unchanged file.

No new gameplay failure was identified. Logs contain nonfatal vanilla sound/shader warnings, brief test-world setup lag, and expected rejection warnings from malformed-packet tests. Long-running multiplayer, unrelated modpacks and a current third-party dependency advisory scan remain outside this check.
