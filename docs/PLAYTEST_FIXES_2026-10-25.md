# October 25 playtest fixes

Version remains **0.1.0-beta**. This follow-up addresses trap interactions, repeated infection audio, iron-golem conversion, medicine-cabinet orientation and underground camp discovery. The complete **322-test server suite**, focused visible trap/combat checks and five-theme clinic-facing checks have passed. Final whole-mod graphical and artifact checks are recorded below.

## Current changes

| Report | Current behavior |
| --- | --- |
| Reset clicks miss closed bear traps | The closed-jaw selection shape reaches the visible jaws. The reset tool can select clean and rusty traps from above or from their sides. |
| A trapped animal or villager intercepts the click | Using the reset tool on the captive releases its own trap before the normal animal/villager interaction consumes the click. The server checks reach, line of sight and modification permission. |
| Player behavior differs between modes | Creative and Spectator players remain immune to trap capture and damage. Survival players can be restrained and released through the native movement and interaction paths. Resetting retains the three-second rearm grace period. |
| Infection audio repeats | The infection sound announces a newly acquired Tetanus infection. Refreshing an existing infection does not replay it. Misses and contacts that cause no infection do not announce an infection. Ordinary actions retain throw/chain/arrow-impact and trap-mechanism sounds; the special Wither uses a vanilla Wither ambient cue. |
| Rusty nails rarely convert iron golems | Rusted projectile exposure uses a dedicated **18% default** golem-conversion chance, covering both player-built and naturally created golems. Successful causal conversion awards **Tetanized Guardian**. Direct application of Rust retains its existing 18% attempt. |
| Clinic cabinets face the wall | Newly generated clinic cabinets face west into the interior aisle, including the biome-themed templates. |
| Hand-placed cabinets face away from the player | New manual placements present the cabinet front to the placer and use that direction when vanilla chooses the hinge. |
| Cave camps are impractical to find | Suitable sites now include roomy, dry Overworld caves beyond lush/dripstone biomes. Widely spaced native grid candidates try nearby cave sites, each with a **15% roll** after the terrain checks. The additional 15% grid rejection was removed. |

The cave search requires a supported natural floor, an open interior and a usable entrance into the surrounding cave. It permits limited edge/roof excavation. Fluid checks cover the complete tent footprint, its horizontal boundary and the exterior layer immediately above it. The original grid spacing, separation and salt are preserved; complete camp footprints remain more than **2,000 blocks apart**, including across old/new camp boundaries on that grid.

The projectile golem chance remains configurable. Disabling general contact conversion also disables the dedicated projectile golem roll; this preserves the existing global conversion-off behavior.

## Finding a camp

Use either of these operator commands to search for distant camps:

```mcfunction
/locate structure tetanusmod:medic_tent
/rustandruin locate tent
```

Once nearby, the field cabinet is also a native point of interest:

```mcfunction
/locate poi tetanusmod:medic_tent
```

The native POI command searches only **128 blocks** around its source. A camp farther away can therefore exist while that command reports no result. Native structure/POI locate feedback displays the horizontal coordinates with `~` for the unknown Y coordinate; the structure coordinate identifies the start chunk rather than the exact cabinet position.

## Verification completed

The three cave regression cases passed in the coordinated native server run:

- The native grid retains its alignment and minimum spacing; ordinary Overworld and cave biome entries are eligible.
- A real noise-generated world uses the native placement calculator and structure-locate pipeline, generates the complete chunks and loads the actual field medic, friendly 200-HP Tetanized Iron Golem, unopened loot chest and dedicated cabinet POI. Native structure locate, the alias and nearby POI locate all succeed.
- The production room-fit predicate rejects water, lava and waterlogged blocks in **6,075 fluid-placement trials across 2,025 boundary/room positions**, including the roof exterior layer. This is a focused geometry regression in addition to the natural-world test.

A separate visible Minecraft client passed camp discovery in a **fresh, normal Overworld with seed 0**. Both global commands found the natural camp **435 blocks** from that player's spawn position. The saved structure occupied X **412-424**, Y **-51 to -44**, Z **160-172**. No camp template, structure start, POI or resident was inserted manually.

The actual client and server reported matching medic and defender UUIDs. The defender had **200 maximum health** and was player-created/friendly. Nearby POI locate identified the field cabinet four blocks away. Opening the natural chest generated **10 matching loot stacks** on client and server. Eight native screenshots cover the command feedback, cabinet, medic, defender and loot.

Local accepted camp evidence is in `.tools/playtest-2026-10-25/camp-pass/`, including `graphical-camp-discovery-plain.json` and its eight screenshots. The probe restored the connected player's original state afterward.

## Final checks

| Check | Status |
| --- | --- |
| Complete current 322-test server suite | PASS: 322 tests, no failures, errors or skips. |
| Focused visible trap/combat/infection-audio checks | PASS: native walking/jumping, three reset clicks, Creative bypass; 11 projectile scenes, six expected and six observed client voice events. |
| Focused clinic/manual cabinet placement checks | PASS: ten native client captures across five themes; native server coverage also includes all rotations and 24 genuine placement combinations plus an obstruction control. |
| Complete current graphical JEI pass | PASS: 29 recipes, 15 potion forms, nine rusting/cleaning pairs, 28 active advancements and six native despawns with four retained controls; 150 fresh frames. |
| Final packaged JAR and release checks | PASS: second 322-test run after forced rebuild; identical JAR; 375 resources regenerated identically twice; package/source agreement, three malformed archives and full MP3/OGG decoding passed. |

The [full graphical gallery](../validation/playtest-2026-10-25/index.html), [focused regression evidence](../validation/playtest-2026-10-25/focused/index.html) and [final artifact/test records](../validation/playtest-2026-10-25/verification/results.json) preserve the current receipts. The final artifact is `rust-and-ruin-0.1.0-beta.jar`, 3,341,616 bytes, SHA-256 `E009BD1E5F88FAFD4E1253562D2F184D68BD3FD68E22DDE8FAAC5010351906D6`. Earlier four-profile October 24 results describe the earlier artifact and do not substitute for this follow-up's final checks.

## Test-fixture corrections

The trap probe originally compared a late player-position snapshot with the position after native movement correction. Passive tick diagnostics showed no escape. The final check uses the stored capture anchor and requires every observed end-of-world-tick sample to retain capture, a closed trap and the same anchor, within the unchanged strict distance threshold. The passing final run also records client positions.

Several failed attempts exposed test assumptions rather than broken camp generation. The structure codec omits a frequency of 1 because it is the default. Minecraft's GameTest server starts a flat world with structures disabled and discards additional datapack dimensions. Its noise-world fixture now uses an isolated native world while preserving the arena's original global generation options.

A FULL chunk request alone did not provide the native entity tracking required by the camp assertion. The test now connects an observer, waits for actual native ticking readiness and resident visibility, then checks the unchanged profession, field-supply marker and defender attributes. Observer and temporary chunk tickets are cleaned up. The graphical probe also stopped waiting for a cabinet block entity: the cabinet is a door block and its readiness is checked through its actual block states. An audio probe needed its observations limited to the target world and scene after natural generation produced unrelated, legitimate infections.

## Existing worlds and practical limits

World-generation changes apply to **newly generated terrain**. Existing buildings and previously saved structure starts are not relocated or rerolled. Existing cabinet facing values are preserved; break and replace an old manually placed cabinet, or inspect a newly generated clinic, to check the orientation fix.

The 15% value is a roll for each suitable site tried within a widely spaced grid cell. It is not a guarantee for every cave or a fixed distance to the nearest camp. The seed-0 result proves that case; other seeds and terrain can differ. Later terrain changes and other world-generation mods can still alter a site.

The review and tests described here support owner playtesting. They do not establish complete bug freedom, compatibility with every modpack or long-running multiplayer stability.
