# Rust & Ruin 0.1 validation

Validated October 18, 2026 with Minecraft 1.20.1, Java 17, Fabric Loader 0.16.14, Fabric API 0.92.6+1.20.1, Loom 1.6.12, and Gradle 8.6. Version remains **0.1.0-beta**. The owner authorized committing this tested expansion on October 18, 2026. It is ready for owner playtesting.

## Results

- Dedicated server: **157 required GameTests passed in each of two consecutive final runs**, with zero failures.
- Graphical clients: **PASS** without a recipe viewer, with JEI 15.3.0.4, and with REI 12.1.725.
- Release build and resource validation: **PASS**. All source resources match the packaged artifact, including the redesigned medic and clinic.
- File audit: all tracked and new nonignored project files pass their applicable format, encoding, image, translation-reference, or local-link checks. The complete inventory is retained locally at `.tools/expansion-file-audit.json`.
- Package auditor rejects deliberately altered resources, duplicate entries, and absolute archive paths. No test classes, recipe-viewer implementations, private files, or unsafe paths are bundled.

Evidence: [first server run](validation/gametest-results.xml), [repeat server run](validation/gametest-results-repeat.xml), [server run summary](validation/server-runs.txt), [client results](validation/client-results.txt), [release integrity](validation/release-audit.json), and [negative package tests](validation/package-negative.json). Logs remain under ignored `.tools/final-expansion-*.log` files.

## Functional coverage

The 157 tests cover diseases, treatment, protection, item use, damage, rusting, recipes, variants, loot, projectiles, advancements, commands, saved state, traps, villagers, and beacon behavior. Specific expansion regressions include:

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

- Size: **3,152,316 bytes**.
- SHA-256: `70B313EBAE18A27FC32200BF3733AECD4F87671E260ECFD3A88F361658302A87`.
- Includes 61 PNGs, audio, recipes, advancements, tags, item/block models, the clinic structure, and required mixins.
- Requires Minecraft 1.20.1, Java 17, Fabric Loader 0.16.14+, and Fabric API on both client and server. JEI and REI are optional.
- Runtime item/status sprites have transparent margins. Editable expansion artwork and building sources are documented in [art provenance](assets-source/art/EXPANSION_ART.md).

## Limits and next testing

These are dedicated/integrated-server functional tests, source/protocol review, adversarial packet tests, and separate release-package checks. They do not establish 100% correctness, arbitrary modpack compatibility, every village terrain layout, or long-running public-server stability. Development runtime tests and the packaged release audit serve different purposes; owner playtesting of this JAR is still required.

Prioritize multiplayer combat, full-duration diseases, glove logout/death handling, naturally generated clinics, trap reset/rusting, partial-inventory smithing, and beacon payment. Subjective appearance, sound volume, and difficulty balance also benefit from playtesting.

See [request audit](docs/REQUEST_AUDIT.md), [security review](docs/SECURITY_AUDIT.md), and [complete audit](docs/FULL_AUDIT.md).
