# Rust & Ruin 0.1 validation

Validated October 17, 2026 with Minecraft 1.20.1, Java 17, Fabric Loader 0.16.14, Fabric API 0.92.6+1.20.1, Loom 1.6.12, and Gradle 8.6. Release version remains **0.1.0-beta**.

## Results

- `tools/Build.ps1 -Test`: **BUILD SUCCESSFUL; all 58 required GameTests passed**, in a dedicated server environment with neither recipe viewer installed.
- `gradlew.bat -PwithJei runClientSmoke --console plain`: **BUILD SUCCESSFUL**, with Fabric JEI 15.3.0.4.
- `gradlew.bat -PwithRei runClientSmoke --console plain`: **BUILD SUCCESSFUL**, with Fabric REI 12.1.725 and its development dependencies.
- The two graphical runs independently displayed all 11 Rusting recipes, the repaired caltrop recipe, potions, species skins, the custom beacon screen, and rusty tab-list hearts.
- Both clients checked that all five requested brewing outputs appear in their viewer: manure base, Tetanus, Salmonella, and both disease splash potions.
- The final clients received the player infection flag and visible Lockjaw Sonata status; the game awarded its advancement and showed its action-bar message.
- Full resource regeneration (`tools/Generate-Resources.ps1`) and a final release build succeeded. JSON, model/texture references, recipe outputs, all three effect icons, and OGG checks passed.

The test report is retained at [validation/gametest-results.xml](validation/gametest-results.xml). Local full logs are ignored files under `.tools/`.

## Final request audit

The [request-by-request audit](docs/REQUEST_AUDIT.md) maps the requested features to their implemented behavior and checks. The audit reproduced and fixed four regressions: legacy golem health, repeat-hit permanent infection, spectator entity interactions, and explosion protection for the special Wither Star. A fifth check verifies that vanilla mob conversions retain permanent mutation.

Five additional GameTests cover those cases, bringing the suite to 58. The spectator fixture explicitly reports spectator mode because Minecraft's built-in mock server player always returns false. The hookline fixture now waits two ticks and verifies its target is present in the world entity lookup before raycasting; an immediate post-spawn raycast intermittently missed during the expanded suite.

JEI and REI graphical checks were rerun against the audited code. The new conversion mixin is included in the release JAR; test and viewer implementation classes remain excluded.

## Gameplay coverage

The original suite still covers nail/rebar damage, consumption and recovery, crafting, container returns, water and rain timing, roofs and interrupted exposure, durability/enchantment/NBT preservation, infection progression and refresh, traps, brushing, grindstones, medicine, and milk.

Expansion and progression checks cover:

- All five iron tool variants, 20% infection, Tetanus I anvil application, Infinity ammunition consumption, physical arrow hits, and the enderman exception.
- Every creatable vanilla mob type can receive permanent mutation state; animals use the 15% boundary. Withers retain separate ritual rules.
- Permanent attribute persistence without stacking, gentle nonlethal periodic mob damage, animal-only infected meat, hostile salvage, safe cooking, and Salmonella symptoms.
- Floating nails rust at the water surface. Rod salvage timing, its 15% hook chance, and duplication prevention remain covered.
- Cow manure collection and cooldown, the complete brewing chain, and gunpowder splash conversions.
- Hookline pull velocity, infection, durability, and cooldown.
- Shapeless caltrop crafting produces four, and the special beacon recipe uses the corrupted star.
- Both new infection advancements and permanent mob conversion credit.
- Lockjaw Sonata is a visible four-second effect independent of random spasms, blocks food use, recurs after 20 seconds, clears when Tetanus is cured, and grants its advancement.
- Rusted golem conversion has exact 18% success/failure boundaries, consumes Rust appropriately, grants both advancements, has 200 health, and persists across saves.
- The special star cleans into a normal Nether Star.
- Ordinary and special Wither structures, failed mutation rolls, doubled boss health, drops, and the consumed netherite-upgraded structure.
- Beacon bases accept gold, netherite, full copper, and waxed/oxidized copper, while rejecting iron/diamond/emerald. The block entity saves and loads with its custom registry identity.
- Beacon payment slots accept only netherite; confirming consumes it and activates effects. An invalid payment cannot change powers.
- Beacon Haste II plus Regeneration II, the Haste III upgrade, and actual placed-block ticks producing Speed III.
- Arrow infection sound plays once at impact, never on bow firing; caltrops use their block position. Custom sound IDs are registered for playsound.

## Visual evidence

Screenshots were visually inspected after the graphical runs:

- [HUD with Lockjaw Sonata](validation/screenshots/rust-and-ruin-jei-hud.png)
- [Rusty tab-list hearts](validation/screenshots/rust-and-ruin-jei-tab-hearts.png)
- [Inventory sprites and potions](validation/screenshots/rust-and-ruin-jei-inventory.png)
- [JEI caltrop recipe](validation/screenshots/rust-and-ruin-jei-caltrops.png)
- [REI caltrop recipe](validation/screenshots/rust-and-ruin-rei-caltrops.png)
- [JEI Rusting category](validation/screenshots/rust-and-ruin-jei-rusting.png)
- [REI Rusting category](validation/screenshots/rust-and-ruin-rei-rusting.png)
- [Netherite-only beacon interface](validation/screenshots/rust-and-ruin-jei-beacon.png)

All item sprites have transparent outer borders. The importer removes neighboring atlas fragments and preserves aspect ratio. Species skins preserve original texture dimensions and UV layouts; sheep wool is tinted separately. Source artwork and exact prompts are retained in [assets-source/art](assets-source/art/PROMPTS.md).

The supplied audio remains normalized, peak-limited mono Vorbis. Sound origins and resource loading were checked; subjective loudness on the user's speakers was not measured.

## Reproduce the client checks

Run the GameTests first. If needed, copy `build/gametest/world` to `build/client-smoke/saves/smoke-world-final`. With JDK 17 configured, run one viewer configuration at a time:

```powershell
.\gradlew.bat -PwithJei runClientSmoke --console plain
.\gradlew.bat -PwithRei runClientSmoke --console plain
```

The harness enters the copied world, prepares the scene, forces late-stage disease for the Lockjaw Sonata check, opens viewer pages and the beacon screen, writes a result file, captures screenshots, and closes Minecraft.

## Release inspection

`build/libs/rust-and-ruin-0.1.0-beta.jar`:

- Display name and version match Rust & Ruin 0.1.0-beta.
- Includes 42 PNGs, normalized audio, recipes, advancements, custom block tags, and required mixins.
- Contains no GameTest, sound-probe, smoke-test, JEI implementation, REI implementation, or Architectury classes.
- Fabric API is the only required mod dependency. JEI and REI are optional integrations.
- Size: 2931865 bytes.
- SHA-256: `49965F052FF08FEA8F432FE5798263CD143F2278DC6506FE766AD67F768D839D`.

## Practical limits

Tests cover a dedicated server environment and graphical integrated-server clients. They do not establish arbitrary modpack compatibility or real-network latency behavior. Dynamic third-party textures use a tint fallback when a source PNG is unavailable. Armor and other covering layers can conceal parts of a skin. Actual probabilities were checked with controlled boundaries rather than thousands of naturally summoned bosses. Subjective multiplayer balance still benefits from playtesting.