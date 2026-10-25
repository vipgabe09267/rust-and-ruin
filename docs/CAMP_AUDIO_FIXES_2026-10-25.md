# Camp discovery and infection audio fixes - October 25, 2026

This pass improves naturally generated cave camps and fixes infection sounds that could be heard too far away or replay during entity maintenance. It also records two world-generation defects found in the actual Minecraft client, beyond the earlier automated checks.

## Commands and discovery

The public command names now use Rust & Ruin:

```text
/locate structure rustandruin:medic_clinic
/locate structure rustandruin:medic_tent
/locate structure rustandruin:workshop
/rustandruin locate medic
/rustandruin locate tent
/rustandruin locate workshop
/locate poi rustandruin:medic
/locate poi rustandruin:medic_tent
```

These remain operator commands requiring permission level 2, including the native `/locate` aliases. Camp structure results include a second message with the exact underground entrance coordinates and a suggested teleport command. The ordinary native structure result identifies its start chunk; its displayed `~` height is not the camp entrance height. POI searches cover only 128 blocks around the command source and inspect already indexed workstations. Use a structure search to find a distant camp. Existing `tetanusmod` command names and saved registry IDs remain supported.

The cave search keeps the original 16 candidate sites and random rolls first, then considers 153 additional sites if those fail. A successful site still needs the same dry, supported cave space and suitable entrance. The per-site chance remains 15 percent, and separate camps retain the minimum 2,000-block spacing. This improves opportunities to find a suitable camp without promising a camp in every region.

A bounded comparison used six seeds and nine native structure-grid regions per seed. It found **15 valid current start plans in 54 regions**, compared with **one legacy plan in 54**. Every sampled seed had at least one current plan; the nearest planned camp centers were approximately **450 to 3,455 blocks** from the benchmark origin. These are native noise/structure-start planning results, **not 15 fully generated, visited camps**. Separate native and graphical tests exercise complete placement and resident loading.

## Camp layout and residents

New camps retain the 13 by 13 by 8 footprint. They have a stone foundation, a tall patched canvas shelter, connected fencing, and two closed gate lines forming an entrance vestibule. The existing stainless-steel field medicine cabinet is preserved. Two beds serve the field medic and a support farmer; a small irrigated, illuminated wheat plot and composter support the farmer. The chest uses the camp loot table through normal native interaction.

The dedicated Tetanized iron golem is player-created, persistent, and has 200 maximum health. It patrols around the center of the camp with an eight-block home radius that includes both residents and the entrance. That camp-only home is serialized and restored because vanilla mob home coordinates do not survive native NBT reload by themselves. The structure also remembers whether its guard was spawned so repeated metadata processing cannot duplicate it. A killed guard is not automatically replaced.

The reported missing guard was **not reproduced as a native despawn defect**: our preserved failing graphical-test world contained its healthy 200-HP guard, both villagers, and their tags. Persistence, a saved patrol home, and enclosure address resident reliability without forcing wild disease variants to persist. Ordinary wild variant despawn behavior remains unchanged.

Native cases cover all four rotations and clipped chunk boundaries; two distinct workstation and bed claims; resident/guard NBT round trips; and a door-breaking zombie kept outside the closed vestibule while both villagers survive. This verifies that specific zombie scenario, not immunity to every possible hostile attack.

## World-generation defects found in the client

The first fresh, normal-world graphical run failed its strict fluid check. Reading the saved chunks showed the farm's intended source was correctly positioned. Four additional source-water blocks instead replaced foundation cells, alongside clay, moss, gravel, dirt and cave plants. This was later lush-cave decoration overwriting natural andesite in the camp floor, rather than water flowing from the farm.

Camp placement now runs in the final generation decoration step, after cave vegetation and springs. A subsequent real-world run had correct irrigation and crops but still revealed neighboring-chunk decoration replacing 12 raw-andesite foundation cells. The final template uses **polished andesite** for those accents, which is outside vanilla's natural stone/moss replacement tags. Native and graphical checks compare all **169 foundation blocks with the actual authored template**, including a repeated graphical check after neighboring chunks and resident ticks have run. Unexpected water is still rejected; only the single explicitly authored irrigation cell is allowed.

The first saved cave also had all eight wheat plants missing, with the farmer still at its authored spawn position and its original seed inventory unchanged. Native crop placement requires sufficient light. Default template postprocessing was checking underground crops before the chunk lighting stage had propagated their lantern light. Camp-only known-shape placement preserves the explicitly authored block states through this pre-light interval. Camp-only fluid replacement also prevents ambient cave water from being retained in its fences and other fixtures. Clinics keep their existing placement policy.

The graphical controller checks eight crops immediately after native generation, live crops after resident ticks, and sufficient native light at every crop cell. The intermediate post-stage run had **eight initial crops, eight live crops, and minimum light 12**. Final polished-foundation verification is listed separately below.

The final polished-floor client run passed with all 169 foundation blocks intact at both observations, eight initial and live wheat plants, minimum crop light 12, and actual client-tracked medic, farmer and 200-HP guard. Native chest interaction generated seven occupied loot stacks. Its original frames and receipts are in the [camp/audio gallery](../validation/camp-audio-2026-10-25/index.html).

A preceding graphical attempt also exposed a test-observation race: chunk block generation finished before the guard's native entity-region loading and indexing. Inspection of that saved world found the healthy tagged guard and `CampGuardSpawned` flag intact. The controller now waits, with a bounded deadline, for native ticking futures and the actual tagged residents before its strict exact-count checks. The final run observed all three residents on its second readiness poll, 74 native world ticks after generation. It neither respawns a guard nor bypasses the requirement to see it in the actual client.

## Positional audio

Normal infection announcements are positional world sounds with a maximum 16-block delivery radius. Configured gain is capped at 1 for this path so a high volume setting cannot also expand that radius; zero volume sends no announcement. A fresh nearby infection can announce once. Refreshing or upgrading an existing episode is quiet. Projectiles and traps use their actual victim/impact or trap position rather than announcing at the thrower or placer.

Naturally initialized or spawner-created variants, inherited conversions, NBT loads and permanent-effect restoration are quiet. Explicit operator infection/conversion remains capable of announcing a genuinely new episode. The guarded silent scope restores its prior state in `finally`, including nested calls. Creating a camp guard also uses that silent initialization path.

The medic has a separate `tetanusmod:medic_work` sound with a 12-block radius. Ordinary work does not play the infection voice. A connected-client check observed no voice 32 blocks away, no replay when approaching an already infected animal, exactly one positional voice for a fresh nearby command infection, no extra voice on refresh, and a separate medic work event. Native packet tests additionally exercise listener boundaries and quiet lifecycle paths.

## Verification status

| Check | Final result |
| --- | --- |
| Earlier native suite before final generation hardening | 332 tests passed; superseded by the 335-test first pass |
| Visible positional-audio client | Passed; one expected infection play and one separate medic work play |
| Intermediate fresh cave after generation-stage/crop fix | Passed; two residents, guard, beds, crops, loot and native commands; later saved-floor review still found replaceable andesite |
| Final native suite with polished foundation and regression cases | **335 required tests passed** in the first final-source run; forced repeat remains pending |
| Final fresh normal-world graphical camp, including late 169-block floor check | **Passed**: 169/169 foundation blocks before and after neighboring chunk readiness, eight initial/live crops, minimum light 12, two villagers and persistent 200-HP guard |
| Full current JEI graphical regression | **PASS: all 29 recipes, 15 potion forms, nine rusting/cleaning pairs, 28 active advancements; six native hostile despawns and four retained controls. 150 fresh screenshots.** |
| Forced final rebuild, repeated native suite and packaged JAR hash | **PASS: identical JAR; all 382 resources regenerated identically twice; source/package checks, three malformed-archive controls and four audio-file full decodes passed.** |

The real cave-client fixture uses seed 0 and an explicitly recorded Creative observer origin at `(-4.5, 140, 14.5)` to reproduce the problematic lush-cave area. It issues real client network commands and loads normally generated chunks. It does not insert templates, structure starts, POI or residents. Creative flight, Night Vision and observer teleports are test preparation; this is not an unassisted Survival exploration run. The original player state is restored afterward. Source PNGs and machine-readable reports are preserved without image editing.

## Compatibility and limits

Existing generated camps retain their saved blocks and residents. There is **no automatic retrofit or terrain replacement** in existing worlds; explore newly generated terrain for the new camp layout. Legacy command names, registry IDs and ordinary clinic placement remain compatible.

The wider search performs more native noise work when the original sites fail. Its cache is bounded and bounded-column samples are upgraded for final full safety checks. Benchmark allocation measurements are **cumulative allocations on the planning thread**, not retained heap, peak process memory, or a guaranteed loading-time improvement. Large short-lived allocations and hardware-dependent search pauses remain a practical cost of searching more suitable cave sites. The fixed six-seed sample and the visited seed-0 camp do not establish a universal discovery distance or exhaustive performance bound.

These checks target concrete failures and regressions. They do not establish that every mod combination, world seed, hostile encounter or long-running server is free of bugs.

Final artifact: `build/libs/rust-and-ruin-0.1.0-beta.jar`, **3,368,181 bytes**, SHA-256 `1D7879422A5CB27CEF1CEC38E5842191F24E05494BBE4927F467AD8C6F9253D9`. Version remains 0.1.0-beta. These latest changes are local and uncommitted for owner testing.

See the [original screenshots and six-seed comparison](../validation/camp-audio-2026-10-25/index.html) and [final verification receipts](../validation/camp-audio-2026-10-25/verification/results.json).
