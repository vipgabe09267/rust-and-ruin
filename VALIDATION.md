# Rust & Ruin 0.1 validation

Validated October 17, 2026 with Minecraft 1.20.1, Java 17, Fabric Loader 0.16.14, Fabric API 0.92.6+1.20.1, Loom 1.6.12, and Gradle 8.6. Version remains **0.1.0-beta**.

## Results

- Dedicated server: **all 96 required GameTests passed**; including the requested complete repeat before committing, with neither recipe viewer installed.
- Graphical JEI 15.3.0.4 and REI 12.1.725 clients: **PASS**.
- Both clients verify the vaccine status and offhand glove synchronization, item-driven guide opening, all ten guide chapters, server protection values, bounded chapter/scroll selection, the three synchronized crafting recipes and an actual vaccine recipe page.
- Both clients receive four deliberately malformed messages through the real custom network handlers, log one warning, and continue all feature checks.
- Retained checks cover all 11 Rusting displays, caltrop crafting, five brewing outputs, 78 vanilla mob base skin layers, six disease potion tooltips, minutes-long Lockjaw, rusty HUD/tab hearts, the custom beacon and rusty advancement wallpaper.
- Release build, resource references, official wrapper checksum, pinned Gradle download, packaging and new item sprite borders pass.

Reports: [GameTests](validation/gametest-results.xml), [client summaries](validation/client-results.txt), [release integrity](validation/release-audit.json), [requested features](docs/REQUEST_AUDIT.md), and [security review](docs/SECURITY_AUDIT.md). Full logs remain ignored under `.tools/`.

## Prevention and security tests

The existing 73 checks retain weapon damage and ammunition, crafting, containers, water/rain rusting, floating nails, roofs, interrupted exposure, data preservation, brushing/grindstones, cures, iron tools, Infinity arrows, permanent variants, animals and salvage, cooking, disease timing, Lockjaw, rituals, commands, advancements, sound positions and beacon powers/payment rules.

Twenty-three additional tests cover:

- Vaccine use consumes exactly one dose, returns one bottle, does not cure or refresh an existing illness, and creates no extra containers in Creative.
- Empty/wrong items, spectators and forged item-duration NBT cannot grant unauthorized vaccination.
- Actual 32-tick drinking completes during Lockjaw, and full Survival inventories drop exactly one bottle.
- Exact 80% vaccine, 50% glove and 90% combined boundaries; glove durability is charged once per exposure.
- Gloves require the offhand, do not protect food/potions, break on their final use and stop protecting once broken.
- Guaranteed prevention rejects direct Tetanus status/potion application and prevents a permanent mutation roll. Blocked refreshes preserve existing disease counters.
- Vaccination does not prevent Salmonella. Saved vaccine status, glove damage and enchantments survive loading; milk removes protection while retaining Tetanus. Vaccination expires naturally.
- Entity-specific checked scopes isolate nested callbacks and restore state after exceptions.
- All three crafting recipes match; vaccine crafting returns exactly two ingredient bottles.
- Reading a guide has no server gameplay effect and consumes no book.
- Hookline cooldown cannot be bypassed by immediate repeated calls, and a solid wall prevents hits.
- Exact presence/symptom payload bounds, strict booleans, invalid stages and valid decoder round trips.
- Bounded configuration parsing, field clamping, escaped quotes/braces, malformed/scalar/null JSON, deeply nested input, oversized/trailing data and **2,000 deterministic random payloads**.
- Non-operators cannot use command helpers or the command tree; non-finite coordinates are rejected.

The real-server-player fixture is used for full-inventory drops. The hookline fixture explicitly clears its positive-test sightline; a separate wall test verifies that obstruction remains enforced. Trap cooldown assertions account for the intended healing when an animal permanently mutates.

## Graphical verification

The harness enters a real integrated-server world, checks the existing mod scene and recipes, opens the advancement tab, closes its container correctly, drinks a vaccine in the server setup, equips a guide/gloves, waits for synchronization, opens the guide using its item action, checks every chapter and displays the vaccine recipe in the installed viewer.

Representative creatures are visually rendered. The harness additionally resolves generated base texture layers for 78 vanilla mob types; the dragon retains its separate hook. It verifies actual symptoms in drinkable, splash and lingering Tetanus/Salmonella tooltips.

Malformed config, deeply nested config, short presence and invalid-stage symptom packets are delivered through Fabric networking. Their handlers reject them and normal gameplay checks continue. Gradle deletes stale result files and fails on a missing/failed fresh result.

Fresh screenshots include:

- [Field guide and server prevention values](validation/screenshots/rust-and-ruin-jei-field-guide.png)
- [REI field guide](validation/screenshots/rust-and-ruin-rei-field-guide.png)
- [JEI vaccine recipe](validation/screenshots/rust-and-ruin-jei-vaccine-recipe.png)
- [REI vaccine recipe](validation/screenshots/rust-and-ruin-rei-vaccine-recipe.png)
- [Lockjaw and rusty hearts](validation/screenshots/rust-and-ruin-jei-hud.png)
- [Rusty advancement wallpaper](validation/screenshots/rust-and-ruin-rei-advancements.png)
- [Custom beacon](validation/screenshots/rust-and-ruin-rei-beacon.png)

## Reproduce

With JDK 17, from the project root:

```powershell
.\tools\Build.ps1 -Test
.\gradlew.bat -PwithJei runClientSmoke --console plain
.\gradlew.bat -PwithRei runClientSmoke --console plain
.\tools\Audit-Release.ps1
```

Run viewers sequentially. The client harness uses `build/client-smoke/saves/smoke-world-final`; if absent, first copy the generated GameTest world there. It prepares the scene, checks assertions, captures screenshots and closes Minecraft.

## Release inspection

`build/libs/rust-and-ruin-0.1.0-beta.jar`:

- Rust & Ruin 0.1.0-beta; Minecraft 1.20.1, Java 17 and Fabric API on both sides. No added mandatory mod dependency.
- Includes 46 PNGs, audio, recipes, advancements, tags, item models and required mixins.
- No test classes, viewer implementations, private files or unsafe archive paths were found.
- New item sprites have transparent margins. Original artwork and exact built-in imagegen prompts are retained under [assets-source/art/prevention](assets-source/art/prevention/PROMPTS.md).
- Strange Meat requires infected-meat pickup; Why did I eat that? requires Salmonella infection.
- Size: 2985006 bytes.
- SHA-256: `E675A7BA4CB52E78D9191F835584E15C3541E1C25D7592C811F91C2FC4C86145`.

## Practical limits

This is a dedicated/integrated-server functional suite, a source/protocol security review, and adversarial packet testing. It does not prove 100% correctness/security, audit every upstream Minecraft/JVM dependency, or establish arbitrary modpack/network compatibility. Texture assertions verify base layers, with representative species visually inspected; third-party renderers that bypass vanilla paths may need compatibility work. Subjective balance and loudness still benefit from multiplayer playtesting.
