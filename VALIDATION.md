# Rust & Ruin 0.1 validation

Validated October 17, 2026 with Minecraft 1.20.1, Java 17, Fabric Loader 0.16.14, Fabric API 0.92.6+1.20.1, Loom 1.6.12, and Gradle 8.6. Release version: **0.1.0-beta**.

## Results

- `tools/Build.ps1 -Test`: **BUILD SUCCESSFUL; all 38 required GameTests passed**, in a dedicated server environment without JEI.
- `gradlew.bat build -PwithJei runClientSmoke --console plain`: **BUILD SUCCESSFUL**. Actual graphical client and integrated server loaded Fabric JEI 15.3.0.4, displayed all 11 Rusting recipes, captured screenshots, and exited normally.
- Resource checks passed for JSON, model references, texture dimensions, recipe outputs, the icon, and OGG audio.
- The graphical log contains no ERROR entries. Two missing vanilla goat sound warnings remain; neither references a mod sound.

## Gameplay coverage

The suite covers crafting layouts and container returns; nail damage, consumption and recovery; melee damage and durability; continuous/interrupted water exposure, rain and roofs; enchantment/NBT preservation; timed infections, stages, refresh and persistence; traps; brushing, grindstones, medicine, and milk.

Expansion coverage includes all five rusted iron tools, 20% melee infection, Tetanus I anvil application, Infinity ammunition consumption, physical arrow and enderman hits, permanent mutation persistence, nonlethal periodic mob damage, doubled Wither health and bonus loot, food safety, and player advancements.

The updated balance checks verify:

- 5% hostile and 2% animal conversion boundaries.
- Normal Withers never randomly mutate when loaded; special rolls run only once.
- Tetanized animals replace ordinary item loot with infected meat only.
- Hostiles drop 1–3 iron nuggets and Rust.
- A brush operation returns exactly one Rust and preserves the cleaned stack.
- Fishing salvage returns two sticks/two string, has the correct 15% hook boundary, and cannot duplicate a destroyed rod.
- A rod survives before five seconds in water and breaks after five seconds.
- Salmonella probability endpoints, hunger loss, light nonlethal damage, update interval, and safe cooked meat.
- The special soul sand recipe matches with one each of Rust, soul sand, and an iron ingot.
- Actual Wither structures: four special blocks can summon a 600-health mutant; ordinary soul sand always summons an ordinary Wither; a failed special roll summons a normal Wither.
- Namespaced summon commands create permanent variants, reject non-mobs, and grant the Wither kill and fishing hook advancements.
- Sound observation in the server: bow firing emits no infection sound; arrow impact emits exactly one at impact; rusty caltrops emit exactly one at their block position. All three custom sound IDs are registered.

Report: `build/gametest-results.xml`. Build logs are local ignored files in `.tools/`.

## Client and artwork

The graphical test displayed custom skins on a cow, zombie, and Wither; the boss bar and rusty player hearts; the Salmonella icon; the placed Tetanized Soul Sand block; all inventory items; and the JEI Rusting category. These screenshots were visually inspected:

- [HUD, skins, special soul sand, and boss bar](validation/screenshots/rust-and-ruin-hud.png)
- [Inventory sprites, effects, and JEI](validation/screenshots/rust-and-ruin-jei.png)
- [JEI Rusting category](validation/screenshots/rust-and-ruin-rusting-jei.png)

Source atlases and the exact built-in image-generation prompts are retained in [assets-source/art](assets-source/art). The supplied mod icon is unchanged. The special soul sand texture is fully opaque; item sprites preserve transparency.

The original MP3 is retained. The game uses normalized, peak-limited mono Vorbis audio; volume defaults to 2.5 and is configurable. Sound resource loading and event origins were checked. Loudness on the user's speakers was not subjectively evaluated.

## Reproduce the graphical test

Run the headless GameTests first. Copy `build/gametest/world` into `build/client-smoke/saves/smoke-world-final` if no smoke world exists. With JDK 17 configured, run:

```powershell
.\gradlew.bat -PwithJei runClientSmoke --console plain
```

The test prepares its scene, opens the inventory and Rusting recipes, writes `build/client-smoke/client-smoke-result.txt`, saves screenshots, and closes the client.

## Release inspection

`build/libs/rust-and-ruin-0.1.0-beta.jar`:

- Name and version match Rust & Ruin 0.1.0-beta.
- Contains 36 PNG assets, normalized audio, recipes, advancements, block tags, and mixins.
- No GameTest, sound-probe, or smoke-test classes are included.
- Fabric API is the only required mod dependency. JEI implementation classes are not bundled.
- Size: 2,843,875 bytes.
- SHA-256: `B865D0FA617CCFC47E37BB888160A836E89C83395EB93C2461937EDE9C218191`.

The tests cover an integrated client/server and a dedicated server environment. Real network latency and compatibility with arbitrary HUD, skin, combat, and loot mods have not been established. Controlled probabilities and ritual tests do not substitute for a long multiplayer balance session.
