# Rust & Ruin 0.1 validation

Validated October 17, 2026 with Minecraft 1.20.1, Java 17, Fabric Loader 0.16.14, Fabric API 0.92.6+1.20.1, Loom 1.6.12, and Gradle 8.6. Version remains **0.1.0-beta**.

## Results

- Dedicated server: **all 73 required GameTests passed**, including a complete repeat after final changes, with neither recipe viewer installed.
- Graphical JEI 15.3.0.4 client: **PASS**.
- Graphical REI 12.1.725 client: **PASS**, using its normal development dependencies.
- Both clients check all 11 Rusting recipes, caltrop crafting, the five requested brewing outputs, rusty HUD/tab hearts, minutes-long Lockjaw, and the custom beacon screen.
- Both clients verify base mutation render layers for **78 vanilla mob types** and the actual symptom text for **all six drinkable/splash/lingering disease potion tooltips**.
- The advancement screen opens with the rusty metal wallpaper. The Salmonella advancement is titled **Why did I eat that?**; meat pickup grants **Strange Meat**.
- Release build and JSON/model/texture/recipe/audio/background reference validation passed.

The server report is retained in [validation/gametest-results.xml](validation/gametest-results.xml), graphical summaries in [validation/client-results.txt](validation/client-results.txt), and the request mapping in [docs/REQUEST_AUDIT.md](docs/REQUEST_AUDIT.md). Full local logs are ignored under `.tools/`.

## Regression and functionality coverage

The 58 earlier tests retain weapon damage, ammo consumption and recovery, crafting/container returns, water/rain timing, floating nails, roofs/interrupted exposure, NBT/durability/enchantments, brushing/grindstones, medicine, iron tools, Infinity arrows, permanent attributes, loot, cooking, rituals, commands, advancements, sound positions and beacon behavior.

Fifteen additional disease tests cover:

- New 300–600 second defaults; migration of old 120–300 defaults, including unversioned configuration; preservation of custom durations.
- Random 120–240 second Lockjaw windows, and long counters surviving save/load.
- Exact Lockjaw onset, a full simulated minute of uninterrupted symptoms, natural expiry and recovery.
- Milk retaining both Tetanus and Lockjaw without restarting progression while still clearing ordinary effects; antitoxin immediately clearing both and returning a bottle.
- Actual deaths across creatable animal types with temporary Tetanus: only infected meat, without ordinary animal loot. Disabled `doMobLoot` is respected.
- Actual infected-meat pickup granting Strange Meat.
- Every ordinary creatable vanilla mob receiving permanent mutation through the shared exposure path under controlled success probabilities, plus an actual rusty melee conversion.
- Random 5–10 minute drinkable Tetanus, unchanged ordinary potion instances, and actual splash/cloud delivery using the same duration range.
- Actual raw-meat consumption with a guaranteed Salmonella roll granting Why did I eat that?; a zero-probability roll granting no such advancement.
- Actual Salmonella ticks causing nausea, hunger loss and mild damage, with nausea applied after status iteration to avoid concurrent modification.

Default mutation probabilities retain existing boundary tests: animals 15%, other mobs 5%, Rust-on-golem 18%, special Wither 5% or 30% with the netherite ritual. Withers remain a separate ritual/command path.

Earlier edge cases remain covered: old golem health migration, repeat-hit permanent infection, spectator interaction rejection, special-star explosion protection, and mutations surviving vanilla mob conversions.

## Graphical checks

Representative animals, spider, zombie, golem and the Tetanized Wither are rendered in the scene. In addition, the harness creates every vanilla mob type and checks the normal living-renderer path resolves to its own generated mutation texture: 78 types. The dragon retains its separate texture hook.

The final tooltip check caught lingering potions using their own vanilla tooltip method; both potion methods are now covered. All six disease tooltips are checked for actual symptom text, and Tetanus no longer displays the misleading nominal 7:30 duration or just a Speed modifier.

The client checks also require a synchronized Lockjaw duration of at least 115 seconds after forcing onset, infection presence synchronization, brewing displays, and the requested Salmonella advancement. The advancement root wallpaper identifier is checked before opening and capturing the tab.

The Gradle client task deletes stale result files and fails if the fresh harness result is missing or reports failure; a clean game shutdown alone is insufficient.

Fresh screenshots include:

- [Lockjaw and rusty hearts](validation/screenshots/rust-and-ruin-jei-hud.png)
- [Tab-list hearts](validation/screenshots/rust-and-ruin-jei-tab-hearts.png)
- [Inventory and potions](validation/screenshots/rust-and-ruin-jei-inventory.png)
- [JEI caltrops](validation/screenshots/rust-and-ruin-jei-caltrops.png)
- [REI caltrops](validation/screenshots/rust-and-ruin-rei-caltrops.png)
- [Custom beacon interface](validation/screenshots/rust-and-ruin-rei-beacon.png)
- [Rusty advancement background](validation/screenshots/rust-and-ruin-rei-advancements.png)

## Reproduce

With JDK 17, from the project root:

```powershell
.\tools\Build.ps1 -Test
.\gradlew.bat -PwithJei runClientSmoke --console plain
.\gradlew.bat -PwithRei runClientSmoke --console plain
```

Run viewer configurations sequentially. The client harness uses `build/client-smoke/saves/smoke-world-final`; if absent, first copy the generated GameTest world there. It prepares its own scene, checks assertions, captures screenshots and closes Minecraft.

## Release inspection

`build/libs/rust-and-ruin-0.1.0-beta.jar`:

- Rust & Ruin 0.1.0-beta metadata; Minecraft 1.20.1, Java 17 and Fabric API required on both sides.
- Includes 42 PNGs, audio, recipes, advancements, block tags and required mixins, including the existing rusty metal tile used as wallpaper.
- Strange Meat uses `inventory_changed`; Why did I eat that? uses `effects_changed` for Salmonella.
- Contains no GameTest/smoke-test classes or bundled JEI, REI or Architectury implementations.
- Size: 2940089 bytes.
- SHA-256: `FF47F014F80D25F93C51DCB097EE3CD48A7F1098E0E97F94D6208505C786926F`.

## Practical limits

These checks cover a dedicated server environment and graphical integrated-server clients. They do not prove arbitrary modpack compatibility, real-network latency behavior or every emergent encounter bug-free. Texture assertions verify generated base render layers, with representative species visually inspected; third-party/procedural renderers that bypass vanilla paths may need compatibility work. Actual probabilities use controlled boundaries rather than thousands of natural boss summons. Subjective loudness and multiplayer balance still benefit from playtesting.
